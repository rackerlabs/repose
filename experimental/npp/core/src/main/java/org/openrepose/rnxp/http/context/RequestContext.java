package org.openrepose.rnxp.http.context;

import org.openrepose.rnxp.servlet.http.UpdatableHttpServletRequest;
import org.openrepose.rnxp.servlet.http.UpdatableHttpServletResponse;

/**
 *
 * @author zinic
 */
public interface RequestContext {

    void startRequest(UpdatableHttpServletRequest request, UpdatableHttpServletResponse response);
    
    void responseConnected(UpdatableHttpServletResponse response);
    
    boolean started();
}
