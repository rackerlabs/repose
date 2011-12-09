package org.openrepose.rnxp.http.context;

import java.util.concurrent.Future;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;
import org.openrepose.rnxp.servlet.http.SwitchableHttpServletResponse;
import org.openrepose.rnxp.servlet.http.live.UpdatableHttpServletResponse;

/**
 *
 * @author zinic
 */
public class SimpleRequestContext implements RequestContext {

    private static boolean INTERRUPT_WORKER_THREAD = Boolean.TRUE;
    
    private final PowerProxy powerProxyInstance;
    private final SwitchableHttpServletResponse response;
    private Future workerThreadFuture;

    public SimpleRequestContext(PowerProxy powerProxyInstance) {
        this.powerProxyInstance = powerProxyInstance;

        response = new SwitchableHttpServletResponse();
    }

    @Override
    public void startRequest(final HttpConnectionController updateController, final OriginConnectionFuture streamController) {
        final RequestDelegate newRequestDelegate = new RequestDelegate(response, updateController, streamController, powerProxyInstance);
        workerThreadFuture = powerProxyInstance.getExecutorService().submit(newRequestDelegate);
    }

    @Override
    public void responseConnected(UpdatableHttpServletResponse newResponse) {
        response.setResponseDelegate(newResponse);
    }

    @Override
    public void conversationAborted() {
        workerThreadFuture.cancel(INTERRUPT_WORKER_THREAD);
    }
}
