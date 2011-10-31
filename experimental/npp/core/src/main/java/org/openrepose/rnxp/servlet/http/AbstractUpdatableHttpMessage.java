package org.openrepose.rnxp.servlet.http;

import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.io.control.UpdatableHttpMessage;
import org.openrepose.rnxp.http.io.control.HttpMessageUpdateController;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.HttpMessageComponentOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public abstract class AbstractUpdatableHttpMessage implements UpdatableHttpMessage {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractUpdatableHttpMessage.class);
    private HttpMessageUpdateController updateController;
    private HttpMessageComponent lastReadComponent;

    // TODO:Review - Visibility
    protected void setUpdateController(HttpMessageUpdateController updateController) {
        this.updateController = updateController;

        lastReadComponent = HttpMessageComponent.MESSAGE_START;
    }

    @Override
    public final void applyPartial(HttpMessagePartial partial) {
        lastReadComponent = partial.getHttpMessageComponent();

        mergeWithPartial(partial);
    }

    @Override
    public final void requestUpdate() {
        try {
            updateController.blockingRequestUpdate(this);
        } catch (InterruptedException ie) {
            LOG.error(ie.getMessage(), ie);
        }
    }

    protected synchronized void loadComponent(HttpMessageComponent component, HttpMessageComponentOrder order) {
        while (order.isEqualOrAfter(component, lastReadComponent())) {
            LOG.info("Requesting more HTTP request data up to " + component + ". Current position: " + lastReadComponent());
            
            requestUpdate();
        }
    }

    protected HttpMessageComponent lastReadComponent() {
        return lastReadComponent;
    }

    protected abstract void mergeWithPartial(HttpMessagePartial partial);
}
