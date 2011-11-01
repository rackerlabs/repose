package org.openrepose.rnxp.http.io.control;

import java.io.InputStream;
import java.io.OutputStream;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.openrepose.rnxp.decoder.partial.ContentMessagePartial;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.netty.valve.ChannelValve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jboss.netty.buffer.ChannelBuffers.*;

/**
 * This controller assumes that the channel is blocked for the duration of logic
 * execution of a message.
 *
 * @author zinic
 */
public class BlockingUpdateController implements HttpMessageUpdateController {

    private static final Logger LOG = LoggerFactory.getLogger(BlockingUpdateController.class);
    private final ChannelValve inboundReadValve;
    private ChannelBuffer contentBuffer;
    private UpdatableHttpMessage updatableMessage;

    public BlockingUpdateController(ChannelValve inboundReadValve) {
        this.inboundReadValve = inboundReadValve;
    }

    @Override
    public void blockingRequestUpdate(UpdatableHttpMessage updatableMessage) throws InterruptedException {
        openInboundValve();

        this.updatableMessage = updatableMessage;

        try {
            LOG.info("Worker thread waiting");

            waitForUpdate();
        } finally {
            updatableMessage = null;
            LOG.info("Released");
        }
    }

    private void initBuffer() {
        if (contentBuffer != null) {
            throw new IllegalStateException("A stream has already been bound to this update controller");
        }

        contentBuffer = buffer(512);
    }
    
    public void commit() {
        shutInboundValve();
    }

    @Override
    public OutputStream connectOutputStream() {
        initBuffer();
        openInboundValve();

        return new ChannelBufferOutputStream(contentBuffer);
    }

    @Override
    public InputStream connectInputStream() {
        initBuffer();
        openInboundValve();

        return new ChannelBufferInputStream(contentBuffer);
    }

    @Override
    public void applyPartial(HttpMessagePartial partial) {
        LOG.info("Applying partial: " + partial.getHttpMessageComponent());

        switch (partial.getHttpMessageComponent()) {
            case CONTENT:
                boolean closeValve = contentBuffer.writable();

                if (!closeValve) {
                    contentBuffer.writeByte(((ContentMessagePartial) partial).getData());
                    closeValve = contentBuffer.writable();
                }

                if (closeValve) {
                    shutInboundValve();
                }

                notifyUpdateWait();

                break;

            // We update for content start and message to let the request know that header parsing is done
            case CONTENT_START:
            case MESSAGE_END_NO_CONTENT:
                shutInboundValve();
                notifyUpdateWait();
                break;

            default:
                // Attempt to apply the partial
                if (updatableMessage != null) {
                    updatableMessage.applyPartial(partial);
                } else {
                    throw new IllegalStateException("Message object not set! No updates are expected at this time.");
                }

                shutInboundValve();
                notifyUpdateWait();
        }
    }

    private void openInboundValve() {
        if (!inboundReadValve.isOpen()) {
            inboundReadValve.open();
        }
    }

    private void shutInboundValve() {
        if (inboundReadValve.isOpen()) {
            inboundReadValve.shut();
        }
    }

    private synchronized void waitForUpdate() throws InterruptedException {
        try {
            wait();
        } catch (InterruptedException ie) {
            throw ie;
        }
    }

    private synchronized void notifyUpdateWait() {
        // Allow the worker thread to continue
        notifyAll();
    }
}
