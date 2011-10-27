package org.openrepose.rnxp.decoder;

import org.openrepose.rnxp.http.HttpHeader;

/**
 *
 * @author zinic
 */
public class HeaderTracker {

    private final HttpHeader[] headerArray;
    private final boolean[] ignoreArray;

    public HeaderTracker() {
        headerArray = HttpHeader.values();
        ignoreArray = new boolean[headerArray.length];
    }
    
    public int nextHeaderSlot() {
        for (int i = 0; i < ignoreArray.length; i++) {
            
        }
        
        return -1;
    }
    
    public int nextHeaderSlot(int firstHeaderSlot) {
        return -1;
    }

    public char[] charArray(int headerSlot) {
        return headerArray[headerSlot].getCharacterArray();
    }

    public void ignore(int headerSlot) {
    }
}
