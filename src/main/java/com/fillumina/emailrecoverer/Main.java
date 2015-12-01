package com.fillumina.emailrecoverer;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("params: [dir:path to scan] [dir:result] " +
                    "[file:log] [own email addresses...]");
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
                    true, // creates files and directory normally
//                    false, // not write anything but print log (debug)
                    addresses)
                    .iterateTree(new File(args[0]));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
