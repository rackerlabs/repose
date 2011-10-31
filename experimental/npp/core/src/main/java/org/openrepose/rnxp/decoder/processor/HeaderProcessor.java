package org.openrepose.rnxp.decoder.processor;

import org.openrepose.rnxp.decoder.partial.impl.HttpErrorPartial;

/**
 *
 * @author zinic
 */
public interface HeaderProcessor {

    HttpErrorPartial processHeader(String key, String value);

    void finishedReadingHeaders();
}
