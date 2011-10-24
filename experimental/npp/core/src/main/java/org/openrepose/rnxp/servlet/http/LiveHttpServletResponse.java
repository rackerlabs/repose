package org.openrepose.rnxp.servlet.http;

import org.openrepose.rnxp.http.domain.HttpPartial;

/**
 *
 * @author zinic
 */
public class LiveHttpServletResponse extends AbstractHttpServletResponse implements UpdatableHttpServletResponse {

    @Override
    protected void mergeWithPartial(HttpPartial partial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
}
