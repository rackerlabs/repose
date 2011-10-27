package org.openrepose.rnxp.http.context;

import javax.servlet.ServletException;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.servlet.http.UpdatableHttpServletRequest;
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
    
    private Thread workerThread;
    private boolean requestStarted;

    public SimpleRequestContext(PowerProxy powerProxyInstance) {
        this.powerProxyInstance = powerProxyInstance;
    }

    @Override
    public synchronized void startRequest(final UpdatableHttpServletRequest request, final UpdatableHttpServletResponse response) {
        workerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    powerProxyInstance.handleRequest(request, response);
                } catch (ServletException se) {
                    LOG.error(se.getMessage(), se);
                }
            }
        });

        workerThread.start();
        requestStarted = true;
    }

    @Override
    public synchronized boolean started() {
        return requestStarted;
    }
}
