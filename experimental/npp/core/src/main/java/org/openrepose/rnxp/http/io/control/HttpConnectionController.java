package org.openrepose.rnxp.http.io.control;

import java.io.InputStream;
import java.io.OutputStream;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.proxy.OutboundCoordinator;

/**
 *
 * @author zinic
 */
public interface HttpConnectionController {

    HttpMessagePartial requestUpdate() throws InterruptedException;
    
    void close();
    
    OutboundCoordinator getCoordinator();
}