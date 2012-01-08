package org.openrepose.rnxp.http.proxy;

import java.net.InetSocketAddress;
import org.openrepose.rnxp.RequestResponsePair;

/**
 *
 * @author zinic
 */
public interface ExternalConnectionFuture {

    void connect(RequestResponsePair requestResponsePair, InetSocketAddress addr) throws InterruptedException ;
}
