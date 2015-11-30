package com.fillumina.emailrecoverer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class TreeNavigator {

    private final Logger log;
    private final OwnAddress ownAddress;
    private final FileFactory fileFactory;
    private final FileParser fileParser;
    private int fileNumber;

    public TreeNavigator(File destDir,
            File logFilename,
            boolean write,
            String[] addresses)
            throws IOException {
        log = new Logger(logFilename, true, write);
        log.print("\n\n\n#### STARTING " + (new Date().toString()) +
                "###\n\n");

        for (String address : addresses) {
            log.print("register local address= " + address);
        }
        ownAddress = new OwnAddress(addresses);

        fileFactory = new FileFactory(destDir, this.log, write);

        fileParser = new FileParser(ownAddress, log, fileFactory, write);
    }

    public void iterateTree(File dir) throws FileNotFoundException, IOException {
        log.print(new Date().toString() +
                ": start parsing tree: " + dir.getAbsolutePath());
        parseTree(dir);
        log.print(new Date().toString() + ": end parsing tree: " +
                dir.getAbsolutePath());
    }

    public void parseTree(File dir) throws FileNotFoundException, IOException {
        log.print(new Date().toString() +
                ": start parsing files in " + dir.getAbsolutePath());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                parseTree(file);
            } else {
                fileNumber++;
                fileParser.parse(file);
                if (fileNumber % 100 == 0) {
                    printStats();
                }
            }
        }
        log.print(new Date().toString() + ": end parsing files in " +
                dir.getAbsolutePath());
    }

    private void printStats() {
        log.print("================ STATISTICS ==================");
        log.print("------------------------------");
        log.print("file parsed: " + fileNumber);
        log.print("email recovered: " + fileParser.getMails());
        log.print("email with fragments: " + fileParser.getFragments());
        log.print("==============================================");
    }
}
