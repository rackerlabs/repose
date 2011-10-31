package org.openrepose.rnxp.http.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author zinic
 */
public interface StreamController {

    void engageRemote(String host, int port);
    
    void commitRequest(HttpServletRequest request);
    
    void commitResponse(HttpServletResponse response);
}
