package org.openrepose.rnxp.http;

import javax.servlet.ServletException;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.servlet.http.UpdatableHttpRequest;
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
    public synchronized void startRequest(final UpdatableHttpRequest request) {
        workerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    powerProxyInstance.handleRequest(request);
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
