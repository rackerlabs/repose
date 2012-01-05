package org.openrepose.rnxp.decoder.partial.impl;

import org.openrepose.rnxp.decoder.partial.StringHttpMessagePartial;
import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public class HttpVersionPartial extends StringHttpMessagePartial {

    public HttpVersionPartial(HttpMessageComponent component, String partialValue) {
        super(component, partialValue);
    }

    public String getHttpVersion() {
        return getPartialValue();
    }
}
