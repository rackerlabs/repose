package org.openrepose.rnxp.http.io.control;

import org.openrepose.rnxp.http.util.StringCharsetEncoder;

/**
 *
 * @author zinic
 */
public enum HttpControlSequence {

    HTTP_VERSION("HTTP/1.1"),
    LINE_END("\r\n"),
    SPACE(" "),
    QUERY_PARAMETER_SEPERATOR("?"),
    HEADER_SEPERATOR(":");
    
    // Class contents
    private final byte[] bytes;

    private HttpControlSequence(String sequence) {
        bytes = StringCharsetEncoder.asciiEncoder().encode(sequence);
    }

    public byte[] getBytes() {
        return bytes;
    }
}
