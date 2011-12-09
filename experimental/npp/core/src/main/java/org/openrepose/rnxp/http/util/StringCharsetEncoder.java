package org.openrepose.rnxp.http.util;

import java.nio.charset.Charset;
import org.jboss.netty.util.CharsetUtil;

/**
 *
 * @author zinic
 */
public class StringCharsetEncoder {

    public static StringCharsetEncoder asciiEncoder() {
        return US_ASCII_CONVERTER;
    }
    
    private static final StringCharsetEncoder US_ASCII_CONVERTER = new StringCharsetEncoder(CharsetUtil.US_ASCII);
    
    private final Charset charset;

    public StringCharsetEncoder(Charset charset) {
        this.charset = charset;
    }

    public byte[] encode(String st) {
        return st.getBytes(charset);
    }

    public byte[] encode(int i) {
        return encode(String.valueOf(i));
    }

    public byte[] encode(long l) {
        return encode(String.valueOf(l));
    }

    public byte[] encode(double d) {
        return encode(String.valueOf(d));
    }

    public byte[] encode(char c) {
        return encode(String.valueOf(c));
    }

    public byte[] encode(boolean b) {
        return encode(String.valueOf(b));
    }
}
