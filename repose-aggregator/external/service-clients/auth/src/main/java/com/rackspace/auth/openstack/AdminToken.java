package com.rackspace.auth.openstack;

import java.util.Calendar;

public class AdminToken {

    /**
     * A convienient location for the admin token cache key used internally to repose.
     */
    public static final String CACHE_KEY = "ADMIN_TOKEN";

    private final String token;
    private final Calendar expires;

    public AdminToken(String token, Calendar expires) {
        this.token = token;
        this.expires = expires;
    }

    public String getToken() {
        return token;
    }

    public boolean isValid() {
        return expires != null && !expires.getTime().before(Calendar.getInstance().getTime());
    }
}
