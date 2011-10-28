package org.openrepose.rnxp.http.io.control;

import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;

/**
 *
 * @author zinic
 */
public interface UpdatableHttpMessage {

    void applyPartial(HttpMessagePartial partial);

    void requestUpdate();

    void setUpdateController(HttpMessageUpdateController updateController);
}
