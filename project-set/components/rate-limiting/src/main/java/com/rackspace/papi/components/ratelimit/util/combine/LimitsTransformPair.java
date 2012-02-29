package com.rackspace.papi.components.ratelimit.util.combine;

import com.rackspace.papi.components.limits.schema.RateLimitList;
import java.io.InputStream;

/**
 *
 * @author zinic
 */
public class LimitsTransformPair {

    private final InputStream is;
    private final RateLimitList rll;

    public LimitsTransformPair(InputStream is, RateLimitList rll) {
        this.is = is;
        this.rll = rll == null ? new RateLimitList() : rll;
    }

    public InputStream getInputStream() {
        return is;
    }

    public RateLimitList getRateLimitList() {
        return rll;
    }
}
