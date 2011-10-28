package org.openrepose.rnxp.decoder.partial;

import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public abstract class AbstractParsedPartial extends AbstractHttpMessagePartial {

    public AbstractParsedPartial(HttpMessageComponent component) {
        super(component);
    }

    @Override
    public boolean isError() {
        return false;
    }
}
