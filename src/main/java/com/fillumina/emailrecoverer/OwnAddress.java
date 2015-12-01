package com.fillumina.emailrecoverer;

/**
 * Useful to separate sent from received messages.
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
