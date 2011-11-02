package org.openrepose.rnxp.servlet.http;

import javax.servlet.http.HttpServletResponse;
import org.openrepose.rnxp.http.io.control.CommittableHttpMessage;

/**
 *
 * @author zinic
 */
public interface CommittableHttpServletResponse extends HttpServletResponse, CommittableHttpMessage {
    
}
