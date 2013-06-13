package com.rackspace.papi.components.clientauth.common;

import org.slf4j.Logger;

/**
 * This class extracts away from the xsd based auto-generated code, giving us more control.
 */

public class EndpointsConfiguration {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationHandler.class);

    private String format; //xml or json
    private Long cacheTimeout; //default is 1 hour
    private Integer identityContractVersion; //not used yet

    public EndpointsConfiguration(String format, Long cacheTimeout, Integer identityContractVersion) {
        this.format = format;
        this.cacheTimeout = cacheTimeout;
        this.identityContractVersion = identityContractVersion;
    }

    public String getFormat() {
        String json = "JSON";

        if (format == null) {
            return json;
        } else {
            return format;
        }
    }

    public void setFormat(String value) {
        this.format = value;
    }

    public long getCacheTimeout() {
        if (cacheTimeout == null) {
            return  600000L;
        } else {
            return cacheTimeout;
        }
    }

    public void setCacheTimeout(Long value) {
        this.cacheTimeout = value;
    }

    //You should be able to configure a contract version, but the only allowed value is "2" :)
    //Probably setting up for later functionality...
    public Integer getIdentityContractVersion() {
        if (identityContractVersion == null || identityContractVersion == 2) {
            return Integer.valueOf(2);
        } else {
            LOG.error("The only supported value is 2.");
            return Integer.valueOf(2);
        }
    }

    public void setIdentityContractVersion(Integer value) {
        this.identityContractVersion = value;
    }
}
