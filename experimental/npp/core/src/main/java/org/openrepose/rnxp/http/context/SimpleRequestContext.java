package org.openrepose.rnxp.http.context;

import javax.servlet.ServletException;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.http.io.control.HttpMessageUpdateController;
import org.openrepose.rnxp.http.proxy.StreamController;
import org.openrepose.rnxp.servlet.http.LiveHttpServletRequest;
import org.openrepose.rnxp.servlet.http.LiveHttpServletResponse;
import org.openrepose.rnxp.servlet.http.UpdatableHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class SimpleRequestContext implements RequestContext {

    private final Logger LOG = LoggerFactory.getLogger(SimpleRequestContext.class);
    private final PowerProxy powerProxyInstance;
    private StreamController streamController;
    private Thread workerThread;
    private boolean requestStarted;

    public SimpleRequestContext(PowerProxy powerProxyInstance) {
        this.powerProxyInstance = powerProxyInstance;
    }

    @Override
    public void startRequest(HttpMessageUpdateController updateController, StreamController streamController) {
        final LiveHttpServletRequest request = new LiveHttpServletRequest(updateController, streamController);
        
        workerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    powerProxyInstance.handleRequest(request, new LiveHttpServletResponse(null));
                } catch (ServletException se) {
                    LOG.error(se.getMessage(), se);
                }
            }
        });

        workerThread.start();
        requestStarted = true;
    }

    @Override
    public void responseConnected(UpdatableHttpServletResponse response) {
        
    }

    @Override
    public synchronized boolean started() {
        return requestStarted;
    }
}
