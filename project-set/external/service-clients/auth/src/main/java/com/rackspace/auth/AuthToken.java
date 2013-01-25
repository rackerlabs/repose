package com.rackspace.auth;

import java.io.Serializable;
import java.util.Calendar;

/**
 * @author fran
 */
public abstract class AuthToken implements Serializable {

    public abstract String getTenantId();
    public abstract String getUserId();
    public abstract String getTokenId();
    public abstract String getUsername();
    public abstract String getRoles();
    public abstract long getExpires();
    public abstract String getImpersonatorTenantId();
    public abstract String getImpersonatorUsername();
    public abstract String getTenantName();
   

    

    public Long tokenTtl() {
        long ttl = 0;

        if (getExpires() > 0) {
            ttl = getExpires() - Calendar.getInstance().getTimeInMillis();
        }

        return ttl > 0 ? ttl : 0;
    }

    public int safeTokenTtl() {
        Long tokenTtl = tokenTtl();

        if (tokenTtl >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return tokenTtl.intValue();
    }
}
