package org.openrepose.rnxp.http.proxy;

import java.net.InetSocketAddress;
import org.openrepose.rnxp.http.io.control.HttpMessageSerializer;

/**
 *
 * @author zinic
 */
public interface OriginConnectionFuture {

    void connect(InetSocketAddress addr, HttpMessageSerializer serializer);
}
