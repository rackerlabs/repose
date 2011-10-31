package org.openrepose.rnxp.servlet.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.io.control.HttpMessageSerializer;
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
    private InputStream connectedInputStream;
    private OutputStream connectedOutputStream;
    private HttpMessageUpdateController updateController;
    private HttpMessageComponent lastReadComponent;

    // TODO:Review - Visibility
    protected void setUpdateController(HttpMessageUpdateController updateController) {
        this.updateController = updateController;

        lastReadComponent = HttpMessageComponent.MESSAGE_START;
    }

    public synchronized ServletInputStream getInputStream() {
        if (connectedInputStream == null) {
            connectedInputStream = updateController.connectInputStream();
        }

        return new ServletInputStream(connectedInputStream);
    }

    public synchronized ServletOutputStream getOutputStream() throws IOException {
        if (connectedOutputStream == null) {
            connectedOutputStream = updateController.connectOutputStream();
        }
        
        return new ServletOutputStream(connectedOutputStream);
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
