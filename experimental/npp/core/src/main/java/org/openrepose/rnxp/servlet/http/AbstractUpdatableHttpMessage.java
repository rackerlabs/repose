package org.openrepose.rnxp.servlet.http;

import org.openrepose.rnxp.http.domain.HttpMessageComponent;
import org.openrepose.rnxp.http.domain.HttpPartial;
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

    @Override
    public final void setUpdateController(HttpMessageUpdateController updateController) {
        this.updateController = updateController;
        
        lastReadComponent = HttpMessageComponent.MESSAGE_START;
    }

    @Override
    public final void applyPartial(HttpPartial partial) {
        lastReadComponent = partial.messageComponent();

        mergeWithPartial(partial);
    }

    @Override
    public final void requestUpdate() {
        try {
            updateController.holdForUpdate(this);
        } catch (InterruptedException ie) {
            LOG.error(ie.getMessage(), ie);
        }
    }

    protected HttpMessageComponent lastReadComponent() {
        return lastReadComponent;
    }

    protected abstract void mergeWithPartial(HttpPartial partial);
}
