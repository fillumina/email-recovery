package com.fillumina.emailrecoverer;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class HexUtils {

    public static String byteArrayToHexString(final byte[] b) {
        final StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }

    /**
     * <b>IMPORTANT:</b> to get the String representation of a byte array
     * you should use this code:
     * <code>String text = <b>new String</b>("pippo".getBytes());</code>
     *
     */
    public static byte[] hexStringToByteArray(final String s) {
        final byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            final int index = i * 2;
            final int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

}
