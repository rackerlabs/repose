package org.openrepose.rnxp.decoder.partial.impl;

import java.io.InputStream;
import org.openrepose.rnxp.decoder.partial.AbstractParsedPartial;
import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public class ContentStartPartial extends AbstractParsedPartial {

    private final InputStream contentInputStream;

    public ContentStartPartial(HttpMessageComponent component, InputStream contentInputStream) {
        super(component);
        this.contentInputStream = contentInputStream;
    }

    public InputStream getContentInputStream() {
        return contentInputStream;
    }
}
