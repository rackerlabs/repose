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
package org.openrepose.core.services.ratelimit.config;

import java.util.List;
import java.util.regex.Pattern;

public class ConfiguredRateLimitWrapper extends ConfiguredRatelimit {

    private static final int PRIME = 31;
    private static final int ZERO = 0;
    private final ConfiguredRatelimit configuredRateLimit;
    private final Pattern regexPattern;

    public ConfiguredRateLimitWrapper(ConfiguredRatelimit configuredRateLimit) {
        this.configuredRateLimit = configuredRateLimit;
        this.regexPattern = Pattern.compile(configuredRateLimit.getUriRegex());
        // todo : optimize so that query params are only computed once (like uri-regex above)
    }

    public Pattern getRegexPattern() {
        return regexPattern;
    }

    @Override
    public String getId() {
        return configuredRateLimit.getId();
    }

    @Override
    public void setId(String value) {
        configuredRateLimit.setId(value);
    }

    @Override
    public String getUri() {
        return configuredRateLimit.getUri();
    }

    @Override
    public void setUri(String value) {
        configuredRateLimit.setUri(value);
    }

    @Override
    public String getUriRegex() {
        return configuredRateLimit.getUriRegex();
    }

    @Override
    public void setUriRegex(String value) {
        configuredRateLimit.setUriRegex(value);
    }

    @Override
    public List<HttpMethod> getHttpMethods() {
        return configuredRateLimit.getHttpMethods();
    }

    @Override
    public int getValue() {
        return configuredRateLimit.getValue();
    }

    @Override
    public void setValue(int value) {
        configuredRateLimit.setValue(value);
    }

    @Override
    public TimeUnit getUnit() {
        return configuredRateLimit.getUnit();
    }

    @Override
    public void setUnit(TimeUnit value) {
        configuredRateLimit.setUnit(value);
    }

    @Override
    public List<QueryParam> getQueryParam() {
        return configuredRateLimit.getQueryParam();
    }

    @Override
    public String toString() {
        return configuredRateLimit.toString();
    }

    @Override
    public boolean equals(Object o) {
        // TODO Does this method need to be updated?
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfiguredRateLimitWrapper that = (ConfiguredRateLimitWrapper) o;

        if (configuredRateLimit != null ? !configuredRateLimit.equals(that.configuredRateLimit) : that.configuredRateLimit != null) {
            return false;
        }

        if (regexPattern != null ? !regexPattern.equals(that.regexPattern) : that.regexPattern != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = configuredRateLimit != null ? configuredRateLimit.hashCode() : ZERO;
        result = PRIME * result + (regexPattern != null ? regexPattern.hashCode() : ZERO);
        return result;
    }
}
