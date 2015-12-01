package com.fillumina.emailrecoverer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class FileParser {
    private static final Pattern HEADER =
            Pattern.compile("^[A-Z][A-Za-z0-9_0\\-]{1,}:\\ ");
    private static final String RECOVERY_HEADER = "X-Recovery-Fix-Import: ";
    private static final boolean SAVE_FRAGMENTS = false; // for debugging

    private static final String[] START_MAIL_HEADERS = new String[] {
        "From - ",
        "From ???@??? ",
        "Return-Path: ",
        "Received: from",
        "X-Mozilla-",
        "X-Spam-",
        "X-Account-Key: ",
        "X-Original-To: ",
        "X-UIDL: ",
        "X-Mozilla-Status: ",
        "X-Mozilla-Status2: ",
        "X-Mozilla-Keys: ",
        "X-Spam-Checker-Version: "
    };

    private final OwnAddress ownAddress;
    private final Logger log;
    private final FileFactory fileFactory;
    private final boolean write;
    private int mails, fragments;

    public FileParser(OwnAddress ownAddress,
            Logger log,
            FileFactory fileFactory,
            boolean write) {
        this.ownAddress = ownAddress;
        this.log = log;
        this.fileFactory = fileFactory;
        this.write = write;
    }

    public void parse(File file) throws IOException {
        final FileLoaderIterator iterator =
                new FileLoaderIterator(new FileReader(file), 0);
        new Inner(file).parse(iterator);
    }

    public int getMails() {
        return mails;
    }

    public int getFragments() {
        return fragments;
    }

    private enum Status {
        HEADER, BODY
    }

    private class Inner {
        private final File file;
        private final List<String> text = new ArrayList<>();
        private String from;
        private String subject;
        private String id;
        private String contentType;
        private String closeBoundary;
        private String openBoundary;
        private Date date;
        private boolean untrustableDate = true;
        private Status status = Status.BODY;
        private int limboBodyLines;

        public Inner(File file) {
            this.file = file;
        }

        private void clearVariables() {
            text.clear();
            from = null;
            subject = null;
            id = null;
            contentType = null;
            closeBoundary = null;
            openBoundary = null;
            date = null;
            untrustableDate = true;
            status = Status.BODY;
            limboBodyLines = 0;
        }

        // passing here an iterable should make it easier to test
        public void parse(Iterable<String> iterable) throws IOException {
            final String absolutePath = file.getAbsolutePath();
            log.print("parsing " + absolutePath + "...");

            int binaryCounter = 0, lastBinaryCounter = 0;
            final List<String> limbo = new ArrayList<>();
            String line;

            for (String row : iterable) {
                line = cleanBinaryData(row, binaryCounter > 0);
                if (line == null) {
                    // binary data
                    binaryCounter += row.length();
                    if (!limbo.isEmpty()) {
                        int size = 0;
                        for (String l : limbo) {
                            size += l.length() + 1; // CR
                        }
                        binaryCounter += size + lastBinaryCounter;
                        limbo.clear();
                        lastBinaryCounter = 0;
                    }
                    continue;
                }

                int length = line.length();
                if (binaryCounter > 0 || (!limbo.isEmpty() && limbo.size() < 5)) {
                    if (line.isEmpty() || // CR within a binary blob
                            length < 5 || // random txt
                            line.indexOf(' ') == -1) { // random
                        binaryCounter += length;
                        continue;
                    }
                    limbo.add(line);
                    log.print("limbo (" + binaryCounter + ")= " + line);
                    lastBinaryCounter = binaryCounter;
                    binaryCounter = 0;
                    continue;
                }

                if (!limbo.isEmpty()) {
                    log.print("binary data size= " + lastBinaryCounter);
                    log.print("file=" + file.getAbsolutePath());
                    log.print("first text after bin: ");
                    log.dump(limbo);

                    for (String l : limbo) {
                        parseLine(l);
                    }

                    lastBinaryCounter = 0;
                    limbo.clear();
                    continue;
                }

                parseLine(line);

                text.add(line);
            }
            checkIfMailIsInBuffer();
        }

        private void parseLine(String line) throws IOException {
            log.printDebugLine(line);

            switch(status) {
                case BODY:
                    // search for a new email
                    if (isStartingHeader(line)) {
                        log.print("start of email");
                        checkIfMailIsInBuffer();
                        status = Status.HEADER;
                        break;
                    } else {
                        limboBodyLines = 0;
                        checkBoundary(line);
                    }
                    break;

                case HEADER:
                    if (isBodyText(line)) {
                        log.print("limbo body");
                        limboBodyLines++;
                        checkBoundary(line);
                        if (limboBodyLines > 2) {
                            // to overcome text noise in headers
                            log.print("starting body");
                            status = Status.BODY;
                        }
                        break;
                    } else {
                        limboBodyLines = 0;
                        readHeader(line);
                    }
                    break;
            }

        }

        private void checkIfMailIsInBuffer() throws IOException {
            if (text.isEmpty() || from == null || date == null) {
                clearVariables();
                return;
            }
            save();
            clearVariables();
        }

        private boolean isBodyText(String line) {
            return !line.startsWith(" ") &&
                    !line.startsWith("\t") &&
                    !HEADER.matcher(line).find();
        }

        private boolean isStartingHeader(String line) {
            for (String header : START_MAIL_HEADERS) {
                if (line.startsWith(header)) {
                    return true;
                }
            }
            return false;
        }

        private void readHeader(String line) throws IOException {
            // use every possible mean to capture a date
            if (date == null && line.startsWith("From - ")) {
                final String dateStr = line.substring(7);
                date = DateExtractor.parse(dateStr);
                if (date != null) {
                    untrustableDate = true;
                    log.print("read date from 'From - '= '" + dateStr +
                            "' parsed as= '" + date.toString() + "'");
                }

            } else if (date == null && line.startsWith("Received: from ")) {
                int idx = line.lastIndexOf(';');
                final String dateStr = line.substring(idx + 1);
                date = DateExtractor.parse(dateStr);
                if (date != null) {
                    untrustableDate = true;
                    log.print("read date from 'Received: from'= '" + dateStr +
                            "' parsed as= '" + date.toString() + "'");
                }

            } else if (from == null && line.startsWith("From: ")) {
                from = line.substring(6);
                log.print("read from= " + from);

            } else if (untrustableDate && line.startsWith("Date: ")) {
                String dateStr = line.substring(6);
                date = DateExtractor.parse(dateStr);
                log.print("read date= '" + dateStr +
                        "' parsed as= '" + date.toString() + "'");
                untrustableDate = false;

            } else if (subject == null && line.startsWith("Subject: ")) {
                subject = line.substring(9);
                log.print("read subject= " + subject);

            } else if (id == null && line.startsWith("Message-ID: ")) {
                if (id != null) {
                    checkIfMailIsInBuffer();
                }
                id = line.substring(11);
                log.print("read id= " + id);

            } else if (line.startsWith("ContentType: ")) {
                contentType = line.substring(13);
                log.print("read content type= " + contentType);

            }
        }

        private void checkBoundary(String line) {
            if (Boundary.isValid(line)) {
                if (Boundary.isClose(line)) {
                    if (openBoundary != null &&
                            line.startsWith(openBoundary)) {
                        log.print("remove open boundary= " + line);
                        openBoundary = null;

                    } else if (closeBoundary == null) {
                        log.print("close boundary = " + line);
                        closeBoundary = line;

                    } else {
                        log.print("unexpected close boundary= " + line);

                    }
                } else {
                    log.print("open boundary= " + line);
                    openBoundary = line;
                }
            }
        }

        private void save() throws IOException {
            if (text.size() < 4) {
                log.print("text not sufficient for email:");
                log.dump(text);
                return;
            }

            log.print("saving email");

            File out;
            if (from != null && date != null) {
                out = createMailFilename();
                if (out != null && !text.isEmpty()) {
                    mails++;
                    fileFactory.saveFile(out, text);
                }

            } else if (SAVE_FRAGMENTS) {
                out = createFragmentFile();
                if (out != null) {
                    fragments++;
                    fileFactory.saveFile(out, text);
                }
            }
        }

        private File createMailFilename() throws IOException {
            File out;
            final String name =
                    DateExtractor.toCompactString(date) + "_" +
                    Hash.hashToString(date + from + id + subject) + "_" +
                    file.getName() + ".msg";
            if (ownAddress.isOwnAddress(from)) {
                log.print("sent");
                out = fileFactory.createInSent(date, name);
            } else {
                log.print("received");
                out = fileFactory.createInRecv(date, name);
            }

            int limit = 4;
            while (out.exists()) {
                log.print("WARNING: existing file, changing name");
                out = new File(out.getAbsoluteFile() + ".msg");
                if (limit == 0) {
                    log.print("too many identical files, bailing out...");
                    return null;
                }
                if (out.getName().length() > 200) {
                    log.print("name too long " + out.getName().length());
                    return null;
                }
                limit--;
            }

            log.print("saving mail" +
                    " from= " + from +
                    ", date= " + date +
                    ", subject= " + subject);

            if (text.isEmpty()) {
                log.print("mail is empty");
                return null;
            }

            fixMail();

            return out;
        }

        private File createFragmentFile() throws IOException {
            File out = fileFactory.createInFrag(file);
            log.print("saving fragment= " + out.getAbsolutePath());
            return out;
        }

        private void fixMail() {

            // adds a legal first line header
            if (!text.get(0).startsWith("From - ")) {
                text.add(0, "From -");
                text.add(1, RECOVERY_HEADER + "'From -' header added");
            }

            // adds an end boundary if needed
            if (openBoundary != null) {
                text.add(openBoundary + "--");
                text.add(1, RECOVERY_HEADER + "close boundary added");
            }
        }
    }

    /**
     * A line might be binary data and must be removed otherwise it can
     * have a burst of binary in a normal text (possibly because of an
     * overwriting of data or indexes created by a format) and should be
     * cleaned out. Most of the time the text is still readable.
     *
     * @param row the line to analyze
     * @param prevIsBinary switch a simpler algorithm that imply it's binary data
     *        at the first non textual character. Sometimes a binary blob
     *        can contain text (i.e. an executable) that can falsely trigger
     *        the text filter.
     * @return <ul>
     * <li>null if the line is binary data
     * <li>string with the binary burst characters cleaned out
     * </ul>
     */
    static String cleanBinaryData(String row, boolean prevIsBinary) {
        int l = row.length();
        if (l > 600) {
            return null;
        }
        int prevIndex = -1, binCounter = 0, last = 0, counter = 0, bursts = 0;
        String after = null;
        char c;
        for (int i=0; i<l; i++) {
            c = row.charAt(i);
            if (c != '\t' && (c < 32 || c > 126)) {
                binCounter++;
                if (prevIsBinary || binCounter > (l/10)+1) {
                    return null;
                }

                if (prevIndex == i-1) {
                    counter++;
                } else {
                    counter = 0;
                }
                prevIndex = i;

            } else if (counter > 3) {
                if (after == null) {
                    // it's a binary burst, eliminate it
                    after= row.substring(0, i-counter);
                    last = i;
                    counter = 0;
                } else {
                    return null; // only 1 burst is allowed
                }
                bursts++;
            }
        }
        if (after != null) {
            return after + row.substring(last, l);
        }
        return row;
    }

}
