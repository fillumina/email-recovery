package com.fillumina.emailrecoverer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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

        for (String address : addresses) {
            log.print("register local address= " + address);
        }
        ownAddress = new OwnAddress(addresses);

        fileFactory = new FileFactory(destDir, this.log, write);

        fileParser = new FileParser(ownAddress, log, fileFactory, write);
    }

    public void iterateTree(File dir) throws FileNotFoundException, IOException {
        log.printTimed("start parsing tree", dir);
        parseDir(dir);
        printStats();
        log.printTimed("end parsing tree", dir);
    }

    private void parseDir(File dir) throws FileNotFoundException, IOException {
        log.printTimed("start parsing files in", dir);
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                parseDir(file);
            } else {
                fileNumber++;
                fileParser.parse(file);
                if (fileNumber % 100 == 0) {
                    printStats();
                }
            }
        }
        log.printTimed("end parsing files in", dir);
    }

    private void printStats() {
        log.print("================ STATISTICS ==================");
        log.print("------------------------------");
        log.print("file parsed: " + fileNumber);
        log.print("email recovered: " + fileParser.getMails());
        log.print("unrecoverable fragments: " + fileParser.getFragments());
        log.print("==============================================");
    }
}
