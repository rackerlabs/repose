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
package org.openrepose.filters.ratelimiting.log;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class LimitLogger {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LimitLogger.class);
    private final String user;
    private final HttpServletRequest request;

    public LimitLogger(String user, HttpServletRequest request) {
        this.user = user;
        this.request = request;
    }

    public void log(String configured, String used) {

        LOG.info("Rate limiting user " + getSanitizedUserIdentification() + " at limit amount " + used + ".");
        LOG.info("User rate limited for request " + request.getMethod() + " " + request.getRequestURL() + ".");
        LOG.info("Configured rate limit is: " + configured);
    }

    public String getSanitizedUserIdentification() {
        String userIdentification = user;

        final String xAuthToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString());

        if (StringUtilities.nullSafeEqualsIgnoreCase(xAuthToken, userIdentification)) {
            final String xForwardedFor = request.getHeader(CommonHttpHeader.X_FORWARDED_FOR.toString());

            userIdentification = xForwardedFor != null ? xForwardedFor : request.getRemoteHost();
        }

        return userIdentification;
    }
}
