package org.openrepose.rnxp.http;

import org.openrepose.rnxp.servlet.http.LiveHttpServletRequest;

/**
 *
 * @author zinic
 */
public interface RequestContext {

    void startRequest(LiveHttpServletRequest request);
    
    boolean started();
}
