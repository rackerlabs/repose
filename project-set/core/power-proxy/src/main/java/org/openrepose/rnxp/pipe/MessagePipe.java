package org.openrepose.rnxp.pipe;

/**
 *
 * @author zinic
 */
public interface MessagePipe<T> {

    T nextMessage() throws PipeOperationInterruptedException;

    T nextMessage(long timeout) throws PipeOperationInterruptedException, PipeOperationTimeoutException;

    void pushMessage(T buffer);
}
