package org.openrepose.rnxp.http;

import org.openrepose.rnxp.servlet.http.UpdatableHttpRequest;

/**
 *
 * @author zinic
 */
public interface RequestContext {

    void startRequest(UpdatableHttpRequest request);
    
    boolean started();
}
