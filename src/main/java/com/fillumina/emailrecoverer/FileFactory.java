package com.fillumina.emailrecoverer;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class FileFactory {
    private final Logger log;
    private final File sent, recv, frag, join;
    private final boolean write;

    public FileFactory(File destDir, Logger log, boolean write) throws IOException {
        this.log = log;
        this.write = write;
        this.sent = createSubdir(new File(destDir, "sent"));
        this.recv = createSubdir(new File(destDir, "recv"));
        this.frag = createSubdir(new File(destDir, "frag"));
        this.join = createSubdir(new File(destDir, "join"));
    }

    public File createInSent(Date date, String name) throws IOException {
        String year = DateExtractor.getYearAsString(date);
        return create(sent, year, name);
    }

    public File createInRecv(Date date, String name) throws IOException {
        String year = DateExtractor.getYearAsString(date);
        return create(recv, year, name);
    }

    public File createInFrag(File src) throws IOException {
        if (write) {
            return File.createTempFile("FRAG_" + src.getName() + "_", ".msg", frag);
        } else {
            return new File("unwritten_temp_" + System.nanoTime());
        }
    }

    public File createInJoin(Mail mail) throws IOException {
        String year = DateExtractor.getYearAsString(mail.getDate());
        return create(join, year, mail.getDestFilename().getName());
    }

    private File create(File dest, String year, String name) throws IOException {
        File subdir = createSubdir(new File(dest, year));
        return new File(subdir, name);
    }

    private File createSubdir(File file) throws IOException {
        if (!file.exists()) {
            log.print("creating " + file.getPath());
            if (write) {
                file.mkdir();
            }
        } else if (!file.isDirectory()) {
            throw new IOException(
                    "expecting dir but was regular file= " +
                            file.getAbsolutePath());
        }
        return file;
    }
}
