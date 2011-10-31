package org.openrepose.rnxp.http.context;

import org.openrepose.rnxp.http.io.control.HttpMessageUpdateController;
import org.openrepose.rnxp.http.proxy.StreamController;
import org.openrepose.rnxp.servlet.http.UpdatableHttpServletResponse;

/**
 *
 * @author zinic
 */
public interface RequestContext {

    void startRequest(HttpMessageUpdateController updateController, StreamController streamController);
    
    void responseConnected(UpdatableHttpServletResponse response);
    
    boolean started();
}
