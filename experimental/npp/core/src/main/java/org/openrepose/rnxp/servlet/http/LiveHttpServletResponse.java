package org.openrepose.rnxp.servlet.http;

import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.io.control.HttpMessageUpdateController;
import org.openrepose.rnxp.http.proxy.StreamController;

/**
 *
 * @author zinic
 */
public class LiveHttpServletResponse extends AbstractHttpServletResponse implements UpdatableHttpServletResponse {
    
    public LiveHttpServletResponse(HttpMessageUpdateController updateController) {
        setUpdateController(updateController);
    }

    @Override
    public StreamController getStreamController() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    protected void mergeWithPartial(HttpMessagePartial partial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
