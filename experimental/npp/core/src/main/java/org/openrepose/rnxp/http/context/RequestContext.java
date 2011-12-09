package org.openrepose.rnxp.http.context;

import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;
import org.openrepose.rnxp.servlet.http.live.UpdatableHttpServletResponse;

/**
 *
 * @author zinic
 */
public interface RequestContext {

    void startRequest(HttpConnectionController updateController, OriginConnectionFuture streamController);

    void responseConnected(UpdatableHttpServletResponse response);
    
    void conversationAborted();
}
