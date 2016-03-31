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

import org.ietf.atom.schema.Link;
import org.ietf.atom.schema.Relation;
import org.openrepose.commons.utils.StringUriUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.http.header.HeaderValueParser;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.servlet.RequestMediaRangeInterrogator;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.systemmodel.Destination;
import org.openrepose.filters.versioning.config.MediaTypeList;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;
import org.openrepose.filters.versioning.schema.VersionChoice;
import org.openrepose.filters.versioning.schema.VersionChoiceList;
import org.openrepose.filters.versioning.util.VersionChoiceFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

public class ConfigurationData {

    private final Map<String, ServiceVersionMapping> serviceMappings;
    private final Map<String, Destination> configuredHosts;

    public ConfigurationData(Map<String, Destination> configuredHosts, Map<String, ServiceVersionMapping> serviceMappings) {
        this.configuredHosts = configuredHosts;
        this.serviceMappings = serviceMappings;
    }

    public Collection<ServiceVersionMapping> getServiceMappings() {
        return serviceMappings.values();
    }

    public Map<String, Destination> getConfiguredHosts() {
        return configuredHosts;
    }

    public Destination getHostForVersionMapping(ServiceVersionMapping mapping) throws VersionedHostNotFoundException {
        final Destination host = configuredHosts.get(mapping.getPpDestId());

        if (host == null) {
            throw new VersionedHostNotFoundException("Endpoint: " + mapping.getPpDestId() + " is not specified in the system model");
        }

        return host;
    }

    public VersionedOriginService getOriginServiceForRequest(HttpServletRequestWrapper request) throws VersionedHostNotFoundException {
        // Check URI first to see if it matches configured host href
        VersionedOriginService targetOriginService = findOriginServiceByUri(request);

        // If version info not in URI look in accept header
        if (targetOriginService == null) {
            final MediaType range = RequestMediaRangeInterrogator.interrogate(request.getRequestURI(),
                    request.getPreferredSplittableHeadersWithParameters(CommonHttpHeader.ACCEPT.toString())).get(0);
            final VersionedMapType currentServiceVersion = getServiceVersionForMediaRange(range);

            if (currentServiceVersion != null) {
                final Destination destination = getHostForVersionMapping(currentServiceVersion.getServiceVersionMapping());
                request.replaceHeader(CommonHttpHeader.ACCEPT.toString(), currentServiceVersion.getMediaType().getBase());
                targetOriginService = new VersionedOriginService(currentServiceVersion.getServiceVersionMapping(), destination);
            }
        }

        return targetOriginService;
    }

    public VersionedOriginService findOriginServiceByUri(HttpServletRequestWrapper request) throws VersionedHostNotFoundException {
        for (Map.Entry<String, ServiceVersionMapping> entry : serviceMappings.entrySet()) {
            final VersionedRequest versionedRequest = new VersionedRequest(request, entry.getValue());

            if (versionedRequest.requestBelongsToVersionMapping()) {
                return new VersionedOriginService(entry.getValue(), getHostForVersionMapping(entry.getValue()));
            }
        }

        return null;
    }

    public VersionChoiceList versionChoicesAsList(HttpServletRequestWrapper request) {
        final VersionChoiceList versionChoices = new VersionChoiceList();

        for (ServiceVersionMapping mapping : getServiceMappings()) {
            final VersionedRequest versionedRequest = new VersionedRequest(request, mapping);
            final VersionChoice choice = new VersionChoiceFactory(mapping).create();
            final Link selfReference = new Link();

            selfReference.setRel(Relation.SELF);
            selfReference.setHref(versionedRequest.asExternalURL());

            choice.getLink().add(selfReference);
            versionChoices.getVersion().add(choice);
        }

        return versionChoices;
    }

    public VersionedMapType getServiceVersionForMediaRange(MediaType preferedMediaRange) {
        org.openrepose.filters.versioning.config.MediaType mediaType;
        for (Map.Entry<String, ServiceVersionMapping> serviceMapping : serviceMappings.entrySet()) {
            mediaType = getMatchingMediaType(serviceMapping.getValue(), preferedMediaRange);
            if (mediaType != null) {
                return new VersionedMapType(serviceMapping.getValue(), mediaType);
            }
        }
        return null;
    }

    public org.openrepose.filters.versioning.config.MediaType getMatchingMediaType(ServiceVersionMapping serviceVersionMapping, MediaType preferedMediaType) {
        final MediaTypeList configuredMediaTypes = serviceVersionMapping.getMediaTypes();
        if (configuredMediaTypes == null) {
            return null;
        }
        for (org.openrepose.filters.versioning.config.MediaType configuredMediaType : configuredMediaTypes.getMediaType()) {
            HeaderValue mediaType = new HeaderValueParser(configuredMediaType.getType()).parse();
            if (preferedMediaType.equalsTo(mediaType)) {
                return configuredMediaType;
            }
        }
        return null;
    }

    public boolean isRequestForVersions(HttpServletRequest uniformResourceInfo) {
        return "/".equals(StringUriUtilities.formatUri(uniformResourceInfo.getRequestURI()));
    }
}
