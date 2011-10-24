package org.openrepose.rnxp.servlet.http;

import org.openrepose.rnxp.http.domain.HttpPartial;

/**
 *
 * @author zinic
 */
public interface UpdatableHttpMessage {

    void applyPartial(HttpPartial partial);

    void requestUpdate();

    void setUpdateController(HttpMessageUpdateController updateController);
}
