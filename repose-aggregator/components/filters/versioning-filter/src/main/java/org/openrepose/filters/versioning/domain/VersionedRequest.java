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
package org.openrepose.filters.versioning.domain;

import org.openrepose.commons.utils.StringUriUtilities;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;

import javax.servlet.http.HttpServletRequest;

import static org.openrepose.commons.utils.StringUriUtilities.indexOfUriFragment;

public class VersionedRequest {

    private final HttpServletRequestWrapper request;
    private final ServiceVersionMapping mapping;
    private final String clientAddressedHost;

    public VersionedRequest(HttpServletRequestWrapper request, ServiceVersionMapping mapping) {
        this.request = request;
        this.mapping = mapping;
        this.clientAddressedHost = request.getHeader(CommonHttpHeader.HOST.toString());
    }

    public ServiceVersionMapping getMapping() {
        return mapping;
    }

    public HttpServletRequestWrapper getRequest() {
        return request;
    }

    public boolean isRequestForRoot() {
        return "/".equals(StringUriUtilities.formatUri(request.getRequestURI()));
    }

    public boolean requestBelongsToVersionMapping() {
        final String requestedUri = StringUriUtilities.formatUri(request.getRequestURI());
        final String versionUri = StringUriUtilities.formatUri(mapping.getId());

        return indexOfUriFragment(requestedUri, versionUri) == 0;
    }

    public boolean requestMatchesVersionMapping() {
        final String requestedUri = StringUriUtilities.formatUri(request.getRequestURI());

        return requestedUri.equals(StringUriUtilities.formatUri(mapping.getId()));
    }

    public String asExternalURL() {
        return request.getRequestURL().toString();
    }

    public String asInternalURL() {
        return StringUtilities.join(request.getScheme() + "://", clientAddressedHost, asInternalURI());
    }

    public String asInternalURI() {
        return removeVersionPrefix(request, mapping.getId());
    }

    private String removeVersionPrefix(HttpServletRequest request, String version) {
        if (request.getRequestURI().charAt(0) != '/') {
            throw new IllegalArgumentException("Request URI must be a URI with a root reference - i.e. the URI must start with '/'");
        }

        final String uri = StringUriUtilities.formatUri(request.getRequestURI());
        final String formattedVersion = StringUriUtilities.formatUri(version);

        if (formattedVersion.length() == 1) {
            return uri;
        }

        final String containedVersion = formattedVersion + "/";

        final int start = uri.indexOf(containedVersion);

        if (start >= 0) {
            return uri.replaceFirst(containedVersion, "/");
        } else if (uri.endsWith(formattedVersion)) {
            return uri.replace(formattedVersion, "/");
        }

        return uri;
    }
}
