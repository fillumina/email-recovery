package com.fillumina.emailrecovery;

import java.io.File;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class Divide {
    private File base;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println(
                    "params: [dir:source tree] [dir:destination tree]");
            return;
        }
        final File base = getFile(args[0]);
        final File starting = getFile(args[1]);
        new Divide(base).iterateTree(starting);
    }

    private static File getFile(String filename) {
        File file = new File(filename);
        if (!file.isDirectory()) {
            throw new IllegalStateException(filename + " must be a directory");
        }
        return file;
    }

    public Divide(File base) {
        this.base = base;
    }

    public void iterateTree(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                iterateTree(file);
            } else {
                dispatch(file);
            }
        }
    }

    private void dispatch(File file) {
        final String extension = extractExtension(file);
        final File destDir = selectDir(extension);
        final File destFile = new File(destDir, file.getName());
        //System.out.println(file.toString() + " ---> " + destFile.toString());
        if (destFile.exists()) {
            throw new IllegalStateException(destFile.toString() +
                    " cannot be overwrited by " + file.toString());
        }
        file.renameTo(destFile);
    }

    private String extractExtension(File file) {
        final String name = file.getName();
        final int indexLastDot = name.lastIndexOf('.');
        if (indexLastDot == -1 || indexLastDot == name.length() - 1) {
            return "other";
        } else {
            return name.substring(indexLastDot + 1).toLowerCase();
        }
    }

    private File selectDir(String extension) {
        final File dst = new File(base, extension);
        if (!dst.exists()) {
            dst.mkdir();
        }
        return dst;
    }
}
