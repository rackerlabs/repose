package org.openrepose.rnxp.http.context;

import javax.servlet.ServletException;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.http.io.control.HttpMessageUpdateController;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;
import org.openrepose.rnxp.servlet.http.LiveHttpServletRequest;
import org.openrepose.rnxp.servlet.http.LiveHttpServletResponse;
import org.openrepose.rnxp.servlet.http.SwitchableHttpServletResponse;
import org.openrepose.rnxp.servlet.http.UpdatableHttpServletResponse;
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
    public void startRequest(final HttpMessageUpdateController updateController, final OriginConnectionFuture streamController) {
        final LiveHttpServletRequest request = new LiveHttpServletRequest(updateController, streamController);
        
        workerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                response.setResponseDelegate(new LiveHttpServletResponse(null));
                
                try {
                    powerProxyInstance.handleRequest(request, response);
                    
                    if (!response.isCommitted()) {
                        // TODO: Flush response
                    }
                } catch (ServletException se) {
                    LOG.error(se.getMessage(), se);
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
