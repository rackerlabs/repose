package org.openrepose.rnxp.decoder.partial;

import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 *
 * @author zinic
 */
public interface HttpMessagePartial {

    HttpMessageComponent getHttpMessageComponent();

    boolean isError();
}
