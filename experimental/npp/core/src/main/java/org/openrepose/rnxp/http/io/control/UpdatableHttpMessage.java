package org.openrepose.rnxp.http.io.control;

import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;

/**
 *
 * @author zinic
 */
public interface UpdatableHttpMessage {

    void applyPartial(HttpMessagePartial partial);

    void requestUpdate();

    HttpMessageSerializer commitMessage();

    OriginConnectionFuture getOriginConnectionFuture();
}
