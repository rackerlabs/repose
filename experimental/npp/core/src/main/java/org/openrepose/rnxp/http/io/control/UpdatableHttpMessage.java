package org.openrepose.rnxp.http.io.control;

import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;

/**
 *
 * @author zinic
 */
public interface UpdatableHttpMessage extends CommittableHttpMessage {

   void applyPartial(HttpMessagePartial partial);
}
