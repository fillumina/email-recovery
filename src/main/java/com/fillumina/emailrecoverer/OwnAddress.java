package com.fillumina.emailrecoverer;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class OwnAddress {
    private final String[] addresses;

    public OwnAddress(String[] addresses) {
        this.addresses = addresses;
    }

    public boolean isOwnAddress(String from) {
        for (String address : addresses) {
            if (from.contains(address)) {
                return true;
            }
        }
        return false;
    }
}
