package org.openrepose.rnxp.decoder.partial;

import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public abstract class AbstractHttpMessagePartial implements HttpMessagePartial {

    private final HttpMessageComponent component;

    public AbstractHttpMessagePartial(HttpMessageComponent component) {
        this.component = component;
    }

    @Override
    public HttpMessageComponent getHttpMessageComponent() {
        return component;
    }
}
