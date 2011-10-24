package org.openrepose.rnxp.http;

import org.openrepose.rnxp.http.domain.HttpPartial;
import org.openrepose.rnxp.servlet.http.HttpMessageUpdateController;
import org.openrepose.rnxp.servlet.http.UpdatableHttpMessage;
import org.openrepose.rnxp.valve.ChannelValve;

/**
 *
 * @author zinic
 */
public class BlockingUpdateController implements HttpMessageUpdateController {

    private final ChannelValve inboundChannelValve;
    private UpdatableHttpMessage updatableMessage;

    public BlockingUpdateController(ChannelValve inboundChannelValve) {
        this.inboundChannelValve = inboundChannelValve;
    }

    @Override
    public synchronized void holdForUpdate(UpdatableHttpMessage updatableMessage) throws InterruptedException {
        if (!inboundChannelValve.isOpen()) {
            inboundChannelValve.open();
        }

        try {
            wait();
        } catch (InterruptedException ie) {
            // Preserve the interrupt
            Thread.currentThread().interrupt();
            
            throw ie;
        } finally {
            updatableMessage = null;
        }
    }

    @Override
    public synchronized void applyPartial(HttpPartial partial) {
        switch (partial.messageComponent()) {
            case REQUEST_METHOD:
            case REQUEST_URI:
            case HTTP_VERSION:
                // We update for the request line components in the case where the request is using each update
                
            case CONTENT_START:
                // We update for content start to let the request know that header parsing is done
                notify();

            default:
                if (updatableMessage != null) {
                    // Always apply the partial
                    updatableMessage.applyPartial(partial);
                } else {
                    throw new IllegalStateException("Message object not set! No updates are expected at this time.");
                }
        }
    }
}
