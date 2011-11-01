package org.openrepose.rnxp.http.context;

import org.openrepose.rnxp.http.io.control.HttpMessageUpdateController;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;
import org.openrepose.rnxp.servlet.http.live.UpdatableHttpServletResponse;

/**
 *
 * @author zinic
 */
public interface RequestContext {

    void startRequest(HttpMessageUpdateController updateController, OriginConnectionFuture streamController);
    
    void responseConnected(UpdatableHttpServletResponse response);
    
    boolean started();
}
