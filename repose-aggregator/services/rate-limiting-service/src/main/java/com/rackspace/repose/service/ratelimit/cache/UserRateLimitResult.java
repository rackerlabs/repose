package com.rackspace.repose.service.ratelimit.cache;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/28/14
 * Time: 1:09 PM
 */
public class UserRateLimitResult extends UserRateLimit {
    private boolean success;

    public UserRateLimitResult(HashMap<String, CachedRateLimit> limitMap, boolean success) {
        super(limitMap);
        this.success = success;
    }

    public boolean getSuccess() {
        return success;
    }
}
