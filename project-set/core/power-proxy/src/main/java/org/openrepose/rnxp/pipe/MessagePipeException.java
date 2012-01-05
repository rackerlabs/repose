package org.openrepose.rnxp.pipe;

/**
 *
 * @author zinic
 */
public class MessagePipeException extends Exception {

    public MessagePipeException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public MessagePipeException(String string) {
        super(string);
    }
}
