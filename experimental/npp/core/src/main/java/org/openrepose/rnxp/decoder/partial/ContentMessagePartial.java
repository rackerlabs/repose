package org.openrepose.rnxp.decoder.partial;

import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public class ContentMessagePartial extends AbstractParsedPartial {

    private final byte data;

    public ContentMessagePartial(byte data) {
        super(HttpMessageComponent.CONTENT);
        this.data = data;
    }

    public ContentMessagePartial(HttpMessageComponent component, byte data) {
        super(component);
        this.data = data;
    }

    public byte getData() {
        return data;
    }
}
