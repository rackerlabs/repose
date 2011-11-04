package org.openrepose.rnxp.http.io.control;

import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;

/**
 *
 * @author zinic
 */
public interface UpdatableHttpMessage extends CommittableHttpMessage {

    void applyPartial(HttpMessagePartial partial);

    OriginConnectionFuture getOriginConnectionFuture();
}
