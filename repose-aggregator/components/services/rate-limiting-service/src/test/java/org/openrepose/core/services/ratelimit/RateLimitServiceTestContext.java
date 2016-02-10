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
package org.openrepose.core.services.ratelimit;

import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.core.services.ratelimit.config.HttpMethod;
import org.openrepose.core.services.ratelimit.config.TimeUnit;

import java.util.List;

public class RateLimitServiceTestContext {
    public static final String SIMPLE_URI_REGEX = "/loadbalancer/.*";
    public static final String COMPLEX_URI_REGEX = "/loadbalancer/vips/.*";
    public static final String GROUPS_URI_REGEX = "/loadbalancer/(.*)/1234";
    public static final String SIMPLE_URI = "*loadbalancer*";
    public static final String COMPLEX_URI = "*loadbalancer/vips*";
    public static final String GROUPS_URI = "*loadbalancer/vips/cap1/1234*";
    public static final String SIMPLE_ID = "12345-ABCDE";
    public static final String COMPLEX_ID = "09876-ZYXWV";

    protected ConfiguredRatelimit newLimitConfig(String limitId, String uri, String uriRegex, List<HttpMethod> methods, List<String> queryNames) {
        final ConfiguredRatelimit configuredRateLimit = new ConfiguredRatelimit();

        configuredRateLimit.setId(limitId);
        configuredRateLimit.setUnit(TimeUnit.HOUR);
        configuredRateLimit.setUri(uri);
        configuredRateLimit.setUriRegex(uriRegex);
        configuredRateLimit.setValue(20);
        for (String qn : queryNames) {
            configuredRateLimit.getQueryParamNames().add(qn);
        }
        for (HttpMethod m : methods) {
            configuredRateLimit.getHttpMethods().add(m);
        }

        return configuredRateLimit;
    }
}
