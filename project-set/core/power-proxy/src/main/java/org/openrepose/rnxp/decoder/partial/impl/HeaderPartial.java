package org.openrepose.rnxp.decoder.partial.impl;

import org.openrepose.rnxp.decoder.partial.AbstractParsedPartial;
import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public class HeaderPartial extends AbstractParsedPartial {

    private final String key, value;

    public HeaderPartial(HttpMessageComponent component, String key, String value) {
        super(component);
        this.key = key;
        this.value = value;
    }

    public String getHeaderKey() {
        return key;
    }

    public String getHeaderValue() {
        return value;
    }
}
