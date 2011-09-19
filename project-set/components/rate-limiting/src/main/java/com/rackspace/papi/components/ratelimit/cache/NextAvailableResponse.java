package com.rackspace.papi.components.ratelimit.cache;

import java.util.Date;

/**
 *
 * @author jhopper
 */
public class NextAvailableResponse {

    private final boolean hasRequests;
    private final Date resetTime;

    public NextAvailableResponse(boolean hasRequests, Date resetTime) {
        this.hasRequests = hasRequests;
        this.resetTime = resetTime;
    }

    public Date getResetTime() {
        return resetTime;
    }

    public boolean hasRequestsRemaining() {
        return hasRequests;
    }
}
