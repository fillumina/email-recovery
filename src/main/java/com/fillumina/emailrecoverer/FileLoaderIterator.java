package com.fillumina.emailrecoverer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class FileLoaderIterator
        extends AbstractReadOnlyIterator<String> {

    private final BufferedReader in;

    public FileLoaderIterator(FileReader fileReader,
            final int topLinesToExclude) {
        super();

        this.in = new BufferedReader(fileReader);

        eatLines(topLinesToExclude);
    }

    /** Override if you need to filter lines. */
    protected boolean reject(String line) {
        return false;
    }

    private void eatLines(final int topLinesToExclude) {
        for (int i = 0; i < topLinesToExclude; i++) {
            try {
                in.readLine();
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    protected String getNext(final String current) {
        try {
            String line;
            do {
                line = in.readLine();
            } while (line != null && reject(line));

            if (line == null) {
                closeFile();
                return null;
            } else {
                return line;
            }
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void closeFile() {
        try {
            if (isNotClosed()) {
                in.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected boolean isNotClosed() {
        try {
            return in.ready();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
