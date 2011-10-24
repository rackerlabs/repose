package org.openrepose.rnxp.servlet.http;

import javax.servlet.http.HttpServletRequest;
import org.openrepose.rnxp.http.domain.HttpPartial;

/**
 *
 * @author zinic
 */
public interface UpdatableHttpRequest extends HttpServletRequest {

    void applyPartial(HttpPartial partial);
}
