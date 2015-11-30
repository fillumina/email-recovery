package com.fillumina.emailrecoverer;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * To use a full fledged log would be an overkill (it should be configured)
 * so this is a simple workaround.
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class Logger implements Closeable {
    private final FileWriter log;
    private final boolean printout, write;

    public Logger(File log, boolean printout, boolean write) throws IOException {
        this.log = write ? new FileWriter(log, true) : null;
        this.printout = printout;
        this.write = write;
        if (!write) {
            print("**************************************************");
            print("*  DEBUG MODE: nothing will be written on disk!  *");
            print("**************************************************");
        }
    }

    public void print(String s) {
        if (printout) {
            System.out.println(s);
        }
        if (write) {
            try {
                log.append(s).append('\n');
                log.flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void printText(String line) {
        print("    | " + line);
    }

    @Override
    public void close() throws IOException {
        print("closing log");
        if (write) {
            log.flush();
            log.close();
        }
    }

    public void dump(List<String> text) {
        for (String t : text) {
            printText(t);
        }
    }
}
