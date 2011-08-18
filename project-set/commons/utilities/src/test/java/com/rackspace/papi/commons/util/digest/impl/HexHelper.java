package com.rackspace.papi.commons.util.digest.impl;

public final class HexHelper {

    private HexHelper() {
    }

    public static String convertToHex(byte[] data) {
        final StringBuilder buf = new StringBuilder();

        for (byte aData : data) {
            int halfbyte = (aData >>> 4) & 0x0F;
            int two_halfs = 0;

            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }

                halfbyte = aData & 0x0F;
            } while (two_halfs++ < 1);
        }

        return buf.toString();
    }
}
