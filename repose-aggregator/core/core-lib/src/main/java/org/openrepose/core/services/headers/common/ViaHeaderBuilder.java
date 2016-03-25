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
package org.openrepose.core.services.headers.common;

import org.openrepose.commons.utils.StringUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

public abstract class ViaHeaderBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ViaHeaderBuilder.class);

    protected abstract String getViaValue(HttpServletRequest request);

    public String buildVia(HttpServletRequest request) {

        StringBuilder builder = new StringBuilder();

        String requestProtocol = request.getProtocol();
        LOG.debug("Request Protocol Received: " + requestProtocol);

        if (!StringUtilities.isBlank(requestProtocol)) {
            builder.append(getProtocolVersion(requestProtocol)).append(getViaValue(request));
        }

        return builder.toString();
    }

    private String getProtocolVersion(String protocol) {
        final String version;

        if (protocol.contains("1.0")) {
            version = "1.0";
        } else {
            version = "1.1";
        }
        return version;
    }
}
