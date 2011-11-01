package org.openrepose.rnxp.http.context;

import java.io.IOException;
import javax.servlet.ServletException;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;
import org.openrepose.rnxp.servlet.http.live.LiveHttpServletRequest;
import org.openrepose.rnxp.servlet.http.SwitchableHttpServletResponse;
import org.openrepose.rnxp.servlet.http.detached.DetachedHttpServletResponse;
import org.openrepose.rnxp.servlet.http.live.UpdatableHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class SimpleRequestContext implements RequestContext {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleRequestContext.class);
    private final PowerProxy powerProxyInstance;
    private final SwitchableHttpServletResponse response;
    private Thread workerThread;
    private boolean requestStarted;

    public SimpleRequestContext(PowerProxy powerProxyInstance) {
        this.powerProxyInstance = powerProxyInstance;
        response = new SwitchableHttpServletResponse();
    }

    @Override
    public void startRequest(final HttpConnectionController updateController, final OriginConnectionFuture streamController) {
        final LiveHttpServletRequest request = new LiveHttpServletRequest(updateController, streamController);

        workerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                response.setResponseDelegate(new DetachedHttpServletResponse(updateController));

                try {
                    powerProxyInstance.handleRequest(request, response);
                    response.flushBuffer();
                } catch (ServletException se) {
                    LOG.error(se.getMessage(), se);
                } catch (IOException ioe) {
                    LOG.error(ioe.getMessage(), ioe);
                } finally {
                    updateController.close();
                    LOG.info("Requesting handling finished");
                }
            }
        });

        workerThread.start();
        requestStarted = true;
    }

    @Override
    public void responseConnected(UpdatableHttpServletResponse newResponse) {
        response.setResponseDelegate(newResponse);
    }

    @Override
    public synchronized boolean started() {
        return requestStarted;
    }
}
