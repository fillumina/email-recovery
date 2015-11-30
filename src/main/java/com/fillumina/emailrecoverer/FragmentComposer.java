package com.fillumina.emailrecoverer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class FragmentComposer {
    private final Logger log;
    private final FileFactory fileFactory;
    private final Map<String, Fragment> openBound = new HashMap<>();
    private final Map<String, Fragment> closeBound = new HashMap<>();
    private final TreeSet<Mail> mails = new TreeSet<>(new Comparator<Mail>() {
        @Override
        public int compare(Mail o1, Mail o2) {
            return o1.getDate().compareTo(o2.getDate());
        }

    });

    public FragmentComposer(Logger log, FileFactory fileFactory) {
        this.log = log;
        this.fileFactory = fileFactory;
    }

    public void manageOpenBoundary(Fragment fragment, String boundary) {
        if (boundary == null) {
            return;
        }
        if (Boundary.isClose(boundary)) {
            throw new AssertionError("expected opening boundary: " + boundary);
        }
        Fragment m = closeBound.get(boundary);
        if (m != null) {
            if (m instanceof Mail) {
                throw new AssertionError("mail cannot chain on open fragments");
            }
            log.print("chaining " + fragment + "  -->  " + m);
            fragment.chain(m);
            closeBound.remove(boundary);
        } else {
            openBound.put(boundary, fragment);
        }
    }

    public void manageCloseBoundary(Fragment fragment, String boundary) {
        if (boundary == null) {
            return;
        }
        if (!Boundary.isClose(boundary)) {
            throw new AssertionError("expected closing boundary: " + boundary);
        }
        final String cleaned = Boundary.removeCloseSimbol(boundary);
        Fragment m = openBound.get(cleaned);
        if (m != null) {
            log.print("chaining " + m + "  -->  " + fragment);
            m.chain(fragment);
            openBound.remove(cleaned);
        } else {
            closeBound.put(cleaned, fragment);
        }
    }

    public void attachFragments() throws IOException {
        log.print("attach fragments");
        for (Mail mail : mails) {
            if (mail.size() > 0) {
                File out = fileFactory.createInJoin(mail);
                log.print("create join file=" + out.getAbsolutePath());
                try (FileWriter writer = new FileWriter(out)) {
                    log.print("mail to join= " + mail.getDestFilename());
                    append(writer, mail.getDestFilename());
                    for (Fragment f : mail) {
                        log.print("append= " + f.getDestFilename());
                        append(writer, f.getDestFilename());
                    }
                    writer.flush();
                    writer.close();
                }
            }
        }
    }

    private void append(FileWriter writer, File src)
            throws FileNotFoundException, IOException {
        FileReader reader = new FileReader(src);
        FileLoaderIterator iterable = new FileLoaderIterator(reader, 0);
        for (String line : iterable) {
            writer.append(line).append('\n');
        }
    }

    void addMail(Mail mail) {
        mails.add(mail);
    }

    Iterable<Mail> getMailWithFragments() {
        return new Iterable<Mail>() {
            @Override
            public Iterator<Mail> iterator() {
                return new Iterator<Mail>() {
                    private Iterator<Mail> it = mails.iterator();
                    private Mail current = increment();

                    @Override
                    public boolean hasNext() {
                        return current != null && current.size() > 0;
                    }

                    private Mail increment() {
                        Mail tmp = null;
                        while (it.hasNext() && (tmp == null || tmp.isEmpty())) {
                            tmp = it.next();
                        }
                        return tmp;
                    }

                    @Override
                    public Mail next() {
                        Mail tmp = current;
                        current = increment();
                        return tmp;
                    }

                };
            }

        };
    }

    public int getMailNumber() {
        return mails.size();
    }
}
