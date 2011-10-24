package org.openrepose.rnxp.http;

import org.openrepose.rnxp.http.domain.HttpPartial;
import org.openrepose.rnxp.servlet.http.HttpMessageUpdateController;
import org.openrepose.rnxp.servlet.http.UpdatableHttpMessage;
import org.openrepose.rnxp.valve.ChannelValve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class BlockingUpdateController implements HttpMessageUpdateController {

    private static final Logger LOG = LoggerFactory.getLogger(BlockingUpdateController.class);
    private final ChannelValve inboundReadValve;
    private UpdatableHttpMessage updatableMessage;

    public BlockingUpdateController(ChannelValve inboundReadValve) {
        this.inboundReadValve = inboundReadValve;
    }

    @Override
    public synchronized void holdForUpdate(UpdatableHttpMessage updatableMessage) throws InterruptedException {
        if (!inboundReadValve.isOpen()) {
            inboundReadValve.open();
        }

        this.updatableMessage = updatableMessage;

        try {
            LOG.info("Worker thread waiting");
            
            wait();
        } catch (InterruptedException ie) {
            // Preserve the interrupt
            Thread.currentThread().interrupt();

            throw ie;
        } finally {
            updatableMessage = null;
            LOG.info("Released");
        }
    }

    @Override
    public synchronized void applyPartial(HttpPartial partial) {
        LOG.info("Applying partial: " + partial.messageComponent());

        switch (partial.messageComponent()) {
            // We update for each request line component
            case REQUEST_METHOD:
            case REQUEST_URI:
            case HTTP_VERSION:


            // We update for content start and message to let the request know that header parsing is done
            case CONTENT_START:
            case MESSAGE_END:
                if (inboundReadValve.isOpen()) {
                    // If the read valve is open, close it so we don't process more of the request
                    inboundReadValve.shut();
                }

                // Allow the worker thread to continue
                notify();

            default:
                // Attempt to apply the partial
                if (updatableMessage != null) {
                    updatableMessage.applyPartial(partial);
                } else {
                    throw new IllegalStateException("Message object not set! No updates are expected at this time.");
                }
        }
    }
}
