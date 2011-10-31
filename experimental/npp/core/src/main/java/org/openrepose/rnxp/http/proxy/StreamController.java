package org.openrepose.rnxp.http.proxy;

import java.net.InetSocketAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author zinic
 */
public interface StreamController {

    void engageRemote(InetSocketAddress addr);
    
    void commitRequest(HttpServletRequest request);
    
    void commitResponse(HttpServletResponse response);
}
