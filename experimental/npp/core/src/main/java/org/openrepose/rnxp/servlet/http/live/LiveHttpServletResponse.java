package org.openrepose.rnxp.servlet.http.live;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import java.io.IOException;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.StatusCodePartial;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;

/**
 *
 * @author zinic
 */
public class LiveHttpServletResponse extends AbstractHttpServletResponse implements UpdatableHttpServletResponse {

    private HttpStatusCode statusCode;

    public LiveHttpServletResponse(HttpConnectionController updateController) {
        setUpdateController(updateController);
    }

    @Override
    public void setStatus(int sc) {
        statusCode = HttpStatusCode.fromInt(sc);
    }

    @Override
    public OriginConnectionFuture getOriginConnectionFuture() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void commitMessage() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void mergeWithPartial(HttpMessagePartial partial) {
        switch (partial.getHttpMessageComponent()) {
            case RESPONSE_STATUS_CODE:
                statusCode = ((StatusCodePartial) partial).getStatusCode();
                break;
        }
    }
}
