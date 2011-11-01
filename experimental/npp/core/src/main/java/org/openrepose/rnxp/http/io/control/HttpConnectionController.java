package org.openrepose.rnxp.http.io.control;

import java.io.InputStream;
import java.io.OutputStream;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;

/**
 *
 * @author zinic
 */
public interface HttpConnectionController {

    void blockingRequestUpdate(UpdatableHttpMessage updatableMessage) throws InterruptedException;
    
    void applyPartial(HttpMessagePartial partial);
    
    void close();
    
    InputStream connectInputStream();
    
    OutputStream connectOutputStream();
}
