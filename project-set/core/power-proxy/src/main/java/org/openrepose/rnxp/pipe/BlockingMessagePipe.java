package org.openrepose.rnxp.pipe;

import java.util.Deque;
import java.util.LinkedList;
import org.openrepose.rnxp.io.push.PushController;

/**
 *
 * @author zinic
 */
public class BlockingMessagePipe<T> implements MessagePipe<T> {

    private static final int PIPE_PRESSURE_THRESHOLD = 5;
    private final Deque<T> messageBuffer;

    private PushController channelPushController;
    
    public BlockingMessagePipe() {
        messageBuffer = new LinkedList<T>();
    }
    
    public void setPushController(PushController pushController) {
        channelPushController = pushController;
    }

    @Override
    public synchronized T nextMessage() throws PipeOperationInterruptedException {
        while (messageBuffer.isEmpty()) {
            // TODO:Review
            channelPushController.requestNext();

            try {
                wait();
            } catch (InterruptedException ie) {
                throw new PipeOperationInterruptedException("Message pipe interrupted while waiting for more data", ie);
            }
        }

        return messageBuffer.removeFirst();
    }

    @Override
    public synchronized T nextMessage(long timeout) throws PipeOperationInterruptedException, PipeOperationTimeoutException {
        if (messageBuffer.isEmpty()) {
            channelPushController.requestNext();

            try {
                wait(timeout);
            } catch (InterruptedException ie) {
                throw new PipeOperationInterruptedException("Message pipe interrupted while waiting for more data", ie);
            }
        }

        final T message = messageBuffer.removeFirst();

        if (message == null) {
            throw new PipeOperationTimeoutException("Message pipe operation timed out");
        }

        return message;
    }

    @Override
    public synchronized void pushMessage(T buffer) {
        messageBuffer.addLast(buffer);

        if (messageBuffer.size() > PIPE_PRESSURE_THRESHOLD) {
            channelPushController.stopMessageFlow();
        }
        
        notify();
    }
}
