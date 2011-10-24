package org.openrepose.rnxp.servlet.http;

import org.openrepose.rnxp.http.domain.HttpPartial;

/**
 *
 * @author zinic
 */
public interface HttpMessageUpdateController {

    void holdForUpdate(UpdatableHttpMessage updatableMessage) throws InterruptedException;
    
    void applyPartial(HttpPartial partial);
}
