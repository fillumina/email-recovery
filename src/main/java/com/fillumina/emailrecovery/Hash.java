package com.fillumina.emailrecovery;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * It's much more secure to use something like jBcrypt
 * {@link http://www.mindrot.org/projects/jBCrypt/ }
 *
 * @see http://www.rgagnon.com/javadetails/java-0400.html
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class Hash {
    private static final MessageDigest MD5_DIGEST;

    static {
        try {
            MD5_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(ex);
        }
    }

    /** The length of the returned String is always 32 */
    public static String hashToString(final String password) {
        MD5_DIGEST.update(password.getBytes());
        final byte[] hash = MD5_DIGEST.digest();
        return byteArrayToHexString(hash);
    }

    private static String byteArrayToHexString(final byte[] b) {
        final StringBuffer sb = new StringBuffer(b.length << 1);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }
}
