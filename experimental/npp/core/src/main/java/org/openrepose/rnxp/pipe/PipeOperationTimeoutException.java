package org.openrepose.rnxp.pipe;

/**
 *
 * @author zinic
 */
public class PipeOperationTimeoutException extends MessagePipeException {

    public PipeOperationTimeoutException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public PipeOperationTimeoutException(String string) {
        super(string);
    }
}
