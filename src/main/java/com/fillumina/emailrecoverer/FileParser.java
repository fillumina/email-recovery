package com.fillumina.emailrecoverer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class FileParser {
    private static final String[] HEADERS = new String[] {
        "From ",
        "Return-Path: ",
        "Received: from",
        "X-Mozilla-",
        "X-Spam-",
        "X-Account-Key: ",
        "X-Original-To: ",
        "Delivered-To: "
    };

    private final OwnAddress ownAddress;
    private final Logger log;
    private final FileFactory fileFactory;
    private final FragmentComposer fragmentComposer;
    private final boolean write;

    public FileParser(OwnAddress ownAddress,
            Logger log,
            FileFactory fileFactory,
            FragmentComposer fragmentComposer,
            boolean write) {
        this.ownAddress = ownAddress;
        this.log = log;
        this.fileFactory = fileFactory;
        this.fragmentComposer = fragmentComposer;
        this.write = write;
    }

    public void parse(File file) throws IOException {
        new Inner(file).parse(new FileLoaderIterator(new FileReader(file), 0));
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
        private boolean searchingHeader = true;

        public Inner(File file) {
            this.file = file;
        }

        private void clearVariables() {
            from = null;
            subject = null;
            id = null;
            contentType = null;
            closeBoundary = null;
            openBoundary = null;
            date = null;
        }

        // passing here an iterable should make it easier to test
        public void parse(Iterable<String> iterable) throws IOException {
            final String absolutePath = file.getAbsolutePath();
            log.print("parsing " + absolutePath + "...");

            int binaryCounter = 0, lastBinaryCounter = 0;
            final List<String> limbo = new ArrayList<>();

            for (String line : iterable) {
                if (isBinary(line, binaryCounter > 0)) {
                    binaryCounter += line.length();
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

                text.add(line);
                //printText(line);
                if (line.isEmpty()) {
                    //print("empty line");
                    continue;
                }

                parseLine(line);
            }
            checkIfMailIsInBuffer();
        }

        private void parseLine(String line) throws IOException {
            // check for starting mail
            if (searchingHeader) {
                for (String header : HEADERS) {
                    if (line.startsWith(header)) {
                        log.print("start of email = " + line);
                        searchingHeader = false;
                        checkIfMailIsInBuffer();
                        break;
                    }
                }
            }

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
                searchingHeader = true;
                from = line.substring(6);
                log.print("read from= " + from);

            } else if (untrustableDate && line.startsWith("Date: ")) {
                searchingHeader = true;
                String dateStr = line.substring(6);
                date = DateExtractor.parse(dateStr);
                log.print("read date= '" + dateStr +
                        "' parsed as= '" + date.toString() + "'");
                untrustableDate = false;

            } else if (subject == null && line.startsWith("Subject: ")) {
                searchingHeader = true;
                subject = line.substring(9);
                log.print("read subject= " + subject);

            } else if (id == null && line.startsWith("Message-ID: ")) {
                if (id != null) {
                    checkIfMailIsInBuffer();
                }
                searchingHeader = true;
                id = line.substring(11);
                log.print("read id= " + id);

            } else if (line.startsWith("ContentType: ")) {
                searchingHeader = true;
                contentType = line.substring(13);
                log.print("read content type= " + contentType);

            } else if (Boundary.isValid(line)) {
                if (Boundary.isClose(line)) {
                    if (openBoundary != null &&
                            line.startsWith(openBoundary)) {
                        log.print("remove open boundary= " + line);
                        openBoundary = null;

                    } else if (closeBoundary == null) {
                        log.print("close boundary (fragment) = " + line);
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

        private void checkIfMailIsInBuffer() throws IOException {
            String lastLine = text.remove(text.size() - 1);
            if (from != null || closeBoundary != null) {
                save();
            }
            text.clear();
            text.add(lastLine);
            clearVariables();
        }

        private void save() throws IOException {
            if (text.size() < 4) {
                log.print("text not sufficient for email:");
                log.dump(text);
                return;
            }

            log.print("saving email");

            File out = null;
            if (from != null && date != null) {
                out = saveMail();
            } else if (openBoundary != null || closeBoundary != null) {
                out = saveFragment();
            } else {
                log.print("Cannot recognize text as a mail fragment, sorry:");
                log.dump(text);
            }

            if (out != null) {
                saveFile(out, text.subList(0, text.size() - 1));
            }
        }

        private File saveMail() throws IOException {
            File out;
            final String name = Mail.createFilename(date, from, id, subject) +
                         "_" + file.getName() + ".msg";
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

            Mail mail = new Mail(file, out,
                    from, subject, date, id, contentType);
            fragmentComposer.add(mail);
            log.print("saving mail= " + mail.toString());
            if (openBoundary != null &&
                    (closeBoundary == null ||
                    !closeBoundary.startsWith(openBoundary))) {
                fragmentComposer.manageOpenBoundary(mail, openBoundary);
            }

            return out;
        }

        private File saveFragment() throws IOException {
            File out = fileFactory.createInFrag(file);
            Fragment fragment = new Fragment(file, out);
            log.print("saving fragment= " + fragment.toString());
            if (closeBoundary == null || openBoundary == null ||
                    !closeBoundary.startsWith(openBoundary)) {
                fragmentComposer.manageOpenBoundary(fragment, openBoundary);
                fragmentComposer.manageCloseBoundary(fragment, closeBoundary);
            }
            return out;
        }
    }

    private static boolean isBinary(String line, boolean prevIsBinary) {
        int l = line.length();
        int counter = 0;
        char c;
        for (int i=0; i<l; i++) {
            c = line.charAt(i);
            // remove code chars but include extended ascii 8bit symbols
            if (c != '\t' && (c < 32 || c > 126)) {
                if (prevIsBinary) {
                    return true;
                }
                counter++;
            }
        }
        return counter > (l/5) + 1;
    }

    protected void saveFile(File out, List<String> text) throws IOException {
        log.print("saving file " + out.toString());
        if (write) {
            try (FileWriter writer = new FileWriter(out)) {
                for (String line : text) {
                    writer.write(line);
                    writer.write('\n');
                }
                // double space to help rebuilding mbox
                writer.write("\n\n");
                writer.flush();
            }
        }
    }
}
