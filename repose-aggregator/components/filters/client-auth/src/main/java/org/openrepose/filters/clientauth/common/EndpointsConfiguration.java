/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.clientauth.common;

import org.slf4j.Logger;

/**
 * This class extracts away from the xsd based auto-generated code, giving us more control.
 */
@Deprecated
public class EndpointsConfiguration {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationHandler.class);

    ;
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

    private Formats determineFormat(String input) {

        return input == null || !input.equalsIgnoreCase(Formats.XML.toString()) ? Formats.JSON : Formats.XML;
    }

    private enum Formats {XML, JSON}
}
