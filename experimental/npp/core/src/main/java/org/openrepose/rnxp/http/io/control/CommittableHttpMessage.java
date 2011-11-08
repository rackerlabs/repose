package org.openrepose.rnxp.http.io.control;

import java.io.IOException;

/**
 *
 * @author zinic
 */
public interface CommittableHttpMessage {

    void commitMessage() throws IOException;
}
