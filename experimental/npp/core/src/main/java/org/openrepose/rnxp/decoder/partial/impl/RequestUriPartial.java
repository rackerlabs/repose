package org.openrepose.rnxp.decoder.partial.impl;

import org.openrepose.rnxp.decoder.partial.StringHttpMessagePartial;
import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public class RequestUriPartial extends StringHttpMessagePartial {

    public RequestUriPartial(HttpMessageComponent componentType, String requestUri) {
        super(componentType, requestUri);
    }

    public String getRequestUri() {
        return getPartialValue();
    }
}
