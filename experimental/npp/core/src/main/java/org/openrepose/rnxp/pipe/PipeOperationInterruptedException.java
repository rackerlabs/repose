package org.openrepose.rnxp.pipe;

/**
 *
 * @author zinic
 */
public class PipeOperationInterruptedException extends MessagePipeException {

    public PipeOperationInterruptedException(String string) {
        super(string);
    }

    public PipeOperationInterruptedException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }
}
