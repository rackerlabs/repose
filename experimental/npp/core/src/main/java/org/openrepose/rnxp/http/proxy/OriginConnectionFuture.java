package org.openrepose.rnxp.http.proxy;

import java.net.InetSocketAddress;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author zinic
 */
public interface OriginConnectionFuture {

    void connect(HttpServletRequest request, InetSocketAddress addr);
}
