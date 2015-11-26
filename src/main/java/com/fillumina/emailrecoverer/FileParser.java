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

    /**
     * This inner class allows to parellelize the class which becomes
     * thread safe.
     */
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

        public void parse(Iterable<String> iterable) throws IOException {
            clearVariables();
            final String absolutePath = file.getAbsolutePath();
            log.print("parsing " + absolutePath + "...");

            int binaryCounter = 0;
            boolean searchingHeader = true;

            for (String row : iterable) {
                if (isBinary(row, binaryCounter > 0)) {
                    binaryCounter += row.length();
                    continue;
                }
                final String line = row.trim();
                if (binaryCounter > 0) {
                    if (line.isEmpty()) {
                        continue; // could be a CR within a binary blob
                    }
                    log.print("binary data size= " + binaryCounter);
                }
                text.add(line);
                //printText(line);
                if (line.isEmpty()) {
                    //print("empty line");
                    continue;
                }

                // header
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

                if (line.startsWith("From: ")) {
                    searchingHeader = true;
                    from = line.substring(6);
                    log.print("read from= " + from);

                } else if (line.startsWith("Date: ")) {
                    searchingHeader = true;
                    String dateStr = line.substring(6);
                    date = DateExtractor.parse(dateStr);
                    log.print("read date from received= '" + dateStr +
                            "' parsed as= '" + date.toString() + "'");

                } else if (line.startsWith("Subject: ")) {
                    searchingHeader = true;
                    subject = line.substring(9);
                    log.print("read subject= " + subject);

                } else if (line.startsWith("Message-ID: ")) {
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
                binaryCounter = 0;
            }
            checkIfMailIsInBuffer();
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
