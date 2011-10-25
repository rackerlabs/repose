package org.openrepose.rnxp.decoder;

/**
 *
 * @author zinic
 */
public class HttpDecoderTestException extends RuntimeException {

    public HttpDecoderTestException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public HttpDecoderTestException(String string) {
        super(string);
    }
}
