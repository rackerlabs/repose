package org.openrepose.rnxp.decoder.partial;

import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public class StringHttpMessagePartial extends AbstractParsedPartial {

    private final String partialValue;

    public StringHttpMessagePartial(HttpMessageComponent component, String partialValue) {
        super(component);
        this.partialValue = partialValue;
    }

    protected String getPartialValue() {
        return partialValue;
    }
}
