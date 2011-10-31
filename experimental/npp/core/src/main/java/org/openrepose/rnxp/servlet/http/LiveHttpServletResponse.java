package org.openrepose.rnxp.servlet.http;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.StatusCodePartial;
import org.openrepose.rnxp.http.io.control.HttpMessageSerializer;
import org.openrepose.rnxp.http.io.control.HttpMessageUpdateController;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;

/**
 *
 * @author zinic
 */
public class LiveHttpServletResponse extends AbstractHttpServletResponse implements UpdatableHttpServletResponse {

    private HttpStatusCode statusCode;

    public LiveHttpServletResponse(HttpMessageUpdateController updateController) {
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
    public HttpMessageSerializer commitMessage() {
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
