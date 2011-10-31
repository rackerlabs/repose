package org.openrepose.rnxp.decoder.partial.impl;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import org.openrepose.rnxp.decoder.partial.AbstractParsedPartial;
import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public class StatusCodePartial extends AbstractParsedPartial {

    private final HttpStatusCode statusCode;

    public StatusCodePartial(HttpStatusCode statusCode) {
        super(HttpMessageComponent.RESPONSE_STATUS_CODE);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
