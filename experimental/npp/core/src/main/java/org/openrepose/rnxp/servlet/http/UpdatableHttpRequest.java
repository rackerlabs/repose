package org.openrepose.rnxp.servlet.http;

import org.openrepose.rnxp.http.domain.HttpPartial;

/**
 *
 * @author zinic
 */
public interface UpdatableHttpRequest {

    void applyPartial(HttpPartial partial);
}
