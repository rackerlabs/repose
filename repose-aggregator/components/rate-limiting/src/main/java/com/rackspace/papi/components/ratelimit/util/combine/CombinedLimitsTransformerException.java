package com.rackspace.papi.components.ratelimit.util.combine;

import javax.xml.transform.TransformerException;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: May 9, 2011
 * Time: 4:27:22 PM
 */
public class CombinedLimitsTransformerException extends TransformerException {
    public CombinedLimitsTransformerException(String message) {
        super(message);
    }
}
