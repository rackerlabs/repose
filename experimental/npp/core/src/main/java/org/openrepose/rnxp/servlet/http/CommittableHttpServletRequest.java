package org.openrepose.rnxp.servlet.http;

import javax.servlet.http.HttpServletRequest;
import org.openrepose.rnxp.http.io.control.CommittableHttpMessage;

/**
 *
 * @author zinic
 */
public interface CommittableHttpServletRequest extends HttpServletRequest, CommittableHttpMessage {
    
}
