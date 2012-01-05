package org.openrepose.rnxp.decoder.partial.impl;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import org.openrepose.rnxp.decoder.partial.AbstractHttpMessagePartial;
import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public class HttpErrorPartial extends AbstractHttpMessagePartial {

    private final HttpStatusCode statusCode;
    private final String message;

    public HttpErrorPartial(HttpMessageComponent component, HttpStatusCode statusCode, String message) {
        super(component);
        this.statusCode = statusCode;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    @Override
    public boolean isError() {
        return true;
    }
}
