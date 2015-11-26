package com.fillumina.emailrecoverer;

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

    /** The length of the returned String is always 32 */
    public static String hashToString(final String password) {
        final byte[] hash = Hash.hashToByteArray(password);
        return HexUtils.byteArrayToHexString(hash);
    }

    /**
     * Always add to the password a fixed salt to void a rainbow attack:
     * <code>hashPassword("fixedSalt" + password);</code>
     * The length of the array returned is always 16.
     *
     * @param password
     * @return
     */
    public static byte[] hashToByteArray(final String password) {
        byte[] hash;
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            hash = md5.digest();
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
        return hash;
    }
}
