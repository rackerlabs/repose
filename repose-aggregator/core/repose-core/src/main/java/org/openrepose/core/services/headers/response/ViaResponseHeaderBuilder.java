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
package org.openrepose.core.services.headers.response;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.services.headers.common.ViaHeaderBuilder;

import javax.servlet.http.HttpServletRequest;

public class ViaResponseHeaderBuilder extends ViaHeaderBuilder {

    private final String reposeVersion;
    private final String viaReceivedBy;

    public ViaResponseHeaderBuilder(String reposeVersion, String configuredViaReceivedBy) {
        this.reposeVersion = reposeVersion;
        this.viaReceivedBy = StringUtilities.isBlank(configuredViaReceivedBy) ? "Repose" : configuredViaReceivedBy;
    }

    @Override
    protected String getViaValue(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder(" ");

        return builder.append(viaReceivedBy).append(" (Repose/").append(reposeVersion).append(")").toString();
    }
}
