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
    private static final boolean WRITE = false;

    private final Logger log;
    private final OwnAddress ownAddress;
    private final FileFactory fileFactory;
    private final FragmentComposer fragmentComposer;
    private final FileParser fileParser;
    private int fileNumber;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("params: [dir:path to scan] [dir:result] " +
                    "[file:log] [mail addresses...]");
            return;
        }
        int addressesNumber = args.length - 3;
        String[] addresses = new String[addressesNumber];
        for (int i=3; i<args.length; i++) {
            addresses[i-3] = args[i];
        }
        try {
            new TreeNavigator(
                    new File(args[1]),
                    new File(args[2]),
                    addresses)
                    .iterateTree(new File(args[0]));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public TreeNavigator(File destDir, File logFilename, String[] addresses)
            throws IOException {
        log = new Logger(logFilename, true, WRITE);
        log.print("\n\n\n#### STARTING " + (new Date().toString()) +
                "###\n\n");

        for (String address : addresses) {
            log.print("register local address= " + address);
        }
        ownAddress = new OwnAddress(addresses);

        fileFactory = new FileFactory(destDir, this.log, WRITE);

        fragmentComposer = new FragmentComposer(log, fileFactory);

        fileParser = new FileParser(ownAddress, log, fileFactory,
                fragmentComposer, WRITE);
    }

    public void iterateTree(File dir) throws FileNotFoundException, IOException {
        log.print(new Date().toString() +
                ": start parsing files in " + dir.getAbsolutePath());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                iterateTree(file);
            } else {
                fileNumber++;
                fileParser.parse(file);
                if (fileNumber % 100 == 0) {
                    printStats();
                }
            }
        }
        log.print(new Date().toString() + ": end parsing files");
        printStats();
        fragmentComposer.attachFragments();
        log.close();
    }

    private void printStats() {
        log.print("================ STATISTICS ==================");
        int mailWithFragments = 0;
        for (Mail mail : fragmentComposer.getMailWithFragments()) {
            if (mail.size() > 0) {
                mailWithFragments++;
                log.print(mail.toString());
            }
        }
        log.print("------------------------------");
        log.print("file parsed: " + fileNumber);
        log.print("email recovered: " + fragmentComposer.getMailNumber());
        log.print("email with fragments: " + mailWithFragments);
        log.print("==============================================");
    }
}
