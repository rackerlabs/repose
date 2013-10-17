package com.rackspace.papi.components.clientauth.common;

import org.slf4j.Logger;

/**
 * This class extracts away from the xsd based auto-generated code, giving us more control.
 */

public class EndpointsConfiguration {

    private enum Formats { XML, JSON };

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationHandler.class);
    private static final long TEN_MINUTES_MILLIS = 600000L;

    private Formats format; //xml or json
    private Long cacheTimeout; //default is 1 hour
    private Integer identityContractVersion; //not used yet

    public EndpointsConfiguration(String format, Long cacheTimeout, Integer identityContractVersion) {
        this.format = determineFormat(format);
        this.cacheTimeout = cacheTimeout;
        this.identityContractVersion = identityContractVersion;
    }

    public String getFormat() {

        return format.toString().toLowerCase();
    }

    public void setFormat(String value) {
        this.format = determineFormat(value);
    }

    public long getCacheTimeout() {
        if (cacheTimeout == null) {
            return TEN_MINUTES_MILLIS;
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

    private Formats determineFormat(String input){

        return input == null || !input.equalsIgnoreCase(Formats.XML.toString()) ? Formats.JSON : Formats.XML;
    }
}
