package org.openrepose.rnxp.servlet.http;

import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;

/**
 *
 * @author zinic
 */
public class LiveHttpServletResponse extends AbstractHttpServletResponse implements UpdatableHttpServletResponse {

    @Override
    protected void mergeWithPartial(HttpMessagePartial partial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
