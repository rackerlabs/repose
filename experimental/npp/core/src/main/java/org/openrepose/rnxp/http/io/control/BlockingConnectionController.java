package org.openrepose.rnxp.http.io.control;

import org.openrepose.rnxp.http.io.netty.ChannelOutputStream;
import org.openrepose.rnxp.logging.ThreadStamp;
import java.util.concurrent.locks.Lock;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.openrepose.rnxp.decoder.partial.ContentMessagePartial;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.netty.buffer.ChannelBuffers.*;
import org.openrepose.rnxp.decoder.AbstractHttpMessageDecoder;
import org.openrepose.rnxp.decoder.HttpMessageDecoder;
import org.openrepose.rnxp.decoder.HttpRequestDecoder;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.openrepose.rnxp.pipe.MessagePipe;
import org.openrepose.rnxp.pipe.PipeOperationInterruptedException;

/**
 * This controller assumes that the channel is blocked for the duration of logic
 * execution of a message.
 *
 * @author zinic
 */
public class BlockingConnectionController implements HttpConnectionController {

    private static final Logger LOG = LoggerFactory.getLogger(BlockingConnectionController.class);
    private final InboundOutboundCoordinator coordinator;
    private final MessagePipe<ChannelBuffer> messagePipe;
    private final HttpMessageDecoder decoder;
    
    private ChannelBuffer remainingData;

    public BlockingConnectionController(InboundOutboundCoordinator coordinator, MessagePipe<ChannelBuffer> messagePipe, HttpMessageDecoder decoder) {
        this.coordinator = coordinator;
        this.messagePipe = messagePipe;
        this.decoder = decoder;
    }

    @Override
    public HttpMessagePartial requestUpdate() throws InterruptedException {
        ThreadStamp.outputThreadStamp(LOG, "Worker processing next message");

        try {
            HttpMessagePartial messagePartial = null;
            
            while (messagePartial == null) {
                if (remainingData != null && remainingData.readable()) {
                    messagePartial = decoder.decode(remainingData);
                } else {
                    ThreadStamp.outputThreadStamp(LOG, "Worker requesting next message object from pipe");
                    remainingData = messagePipe.nextMessage();
                }
            }
            
            return messagePartial;
        } catch (PipeOperationInterruptedException poie) {
            throw new RuntimeException(); // TODO:Implement
        } finally {
            ThreadStamp.outputThreadStamp(LOG, "Worker released");
        }
    }

    @Override
    public void close() {
    }

    public void commit() {
    }

    @Override
    public OutputStream connectOutputStream() {
        return new ChannelOutputStream(coordinator);
    }

    @Override
    public InputStream connectInputStream() {
        return new ChannelBufferInputStream(null);
    }
}