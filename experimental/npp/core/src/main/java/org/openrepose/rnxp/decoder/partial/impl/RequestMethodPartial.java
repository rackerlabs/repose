package org.openrepose.rnxp.decoder.partial.impl;

import org.openrepose.rnxp.decoder.partial.AbstractParsedPartial;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.HttpMethod;

/**
 *
 * @author zinic
 */
public class RequestMethodPartial extends AbstractParsedPartial {

    private final HttpMethod httpMethod;

    public RequestMethodPartial(HttpMessageComponent componentType, HttpMethod httpMethod) {
        super(componentType);
        this.httpMethod = httpMethod;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }
}
