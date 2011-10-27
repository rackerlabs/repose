package org.openrepose.rnxp.http;

/**
 *
 * @author zinic
 */
public enum HttpHeader {

    TRANSFER_ENCODING("transfer-encoding".toCharArray()),
    CONTENT_LENGTH("content-length".toCharArray());
    
    // Class contents
    private final char[] characterArray;

    private HttpHeader(char[] characterArray) {
        this.characterArray = characterArray;
    }

    public char[] getCharacterArray() {
        return characterArray;
    }
}
