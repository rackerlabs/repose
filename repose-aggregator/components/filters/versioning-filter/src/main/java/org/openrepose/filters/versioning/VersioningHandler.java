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
package org.openrepose.filters.versioning;

import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.servlet.RequestMediaRangeInterrogator;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.commons.utils.servlet.http.RouteDestination;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.core.filters.Versioning;
import org.openrepose.core.services.reporting.metrics.MeterByCategory;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.filters.versioning.domain.ConfigurationData;
import org.openrepose.filters.versioning.domain.VersionedHostNotFoundException;
import org.openrepose.filters.versioning.domain.VersionedOriginService;
import org.openrepose.filters.versioning.domain.VersionedRequest;
import org.openrepose.filters.versioning.schema.ObjectFactory;
import org.openrepose.filters.versioning.schema.VersionChoiceList;
import org.openrepose.filters.versioning.util.ContentTransformer;
import org.openrepose.filters.versioning.util.VersionChoiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

public class VersioningHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = LoggerFactory.getLogger(VersioningHandler.class);
    private static final ObjectFactory VERSIONING_OBJECT_FACTORY = new ObjectFactory();
    private static final double VERSIONING_DEFAULT_QUALITY = 0.5;
    private final ConfigurationData configurationData;
    private final ContentTransformer transformer;
    private MeterByCategory mbcVersionedRequests;

    public VersioningHandler(ConfigurationData configurationData, ContentTransformer transformer, MetricsService metricsService) {
        this.configurationData = configurationData;
        this.transformer = transformer;

        // TODO replace "versioning" with filter-id or name-number in sys-model
        if (metricsService != null) {
            mbcVersionedRequests = metricsService.newMeterByCategory(Versioning.class,
                    "versioning", "VersionedRequest", TimeUnit.SECONDS);
        }
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector filterDirector = new FilterDirectorImpl();
        final HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request);

        try {
            final VersionedOriginService targetOriginService = configurationData.getOriginServiceForRequest(wrappedRequest, filterDirector);

            if (targetOriginService != null) {
                final VersionedRequest versionedRequest = new VersionedRequest(wrappedRequest, targetOriginService.getMapping());
                handleVersionedRequest(versionedRequest, filterDirector, targetOriginService);
                if (mbcVersionedRequests != null) {
                    mbcVersionedRequests.mark(targetOriginService.getMapping().getId());
                }
            } else {
                handleUnversionedRequest(wrappedRequest, filterDirector);
                if (mbcVersionedRequests != null) {
                    mbcVersionedRequests.mark("Unversioned");
                }
            }
            filterDirector.responseHeaderManager().appendHeader("Content-Type", getPreferredMediaRange(wrappedRequest).getMimeType().getMimeType());
        } catch (VersionedHostNotFoundException vhnfe) {
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_GATEWAY);
            filterDirector.setFilterAction(FilterAction.RETURN);

            LOG.warn("Configured versioned service mapping refers to a bad pp-dest-id. Reason: " + vhnfe.getMessage(), vhnfe);
        } catch (MalformedURLException murlex) {
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_GATEWAY);
            filterDirector.setFilterAction(FilterAction.RETURN);

            LOG.warn("Configured versioned service mapping refers to a bad host definition. Reason: " + murlex.getMessage(), murlex);
        }

        // This is not a version we recognize - tell the client what's up
        if (filterDirector.getFilterAction() == FilterAction.NOT_SET) {
            writeMultipleChoices(filterDirector, wrappedRequest);
        }

        return filterDirector;
    }

    private void handleUnversionedRequest(HttpServletRequestWrapper request, FilterDirector filterDirector) {
        // Is this a request to the service root to describe the available versions? (e.g. http://api.service.com/)
        if (configurationData.isRequestForVersions(request)) {
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_OK);
            filterDirector.setFilterAction(FilterAction.RETURN);

            final JAXBElement<VersionChoiceList> versions = VERSIONING_OBJECT_FACTORY.createVersions(configurationData.versionChoicesAsList(request));
            transformer.transform(versions, getPreferredMediaRange(request), filterDirector.getResponseOutputStream());
        }
    }

    private void handleVersionedRequest(VersionedRequest versionedRequest, FilterDirector filterDirector, VersionedOriginService targetOriginService) throws VersionedHostNotFoundException, MalformedURLException {
        // Is this a request to a version root we are aware of for describing it? (e.g. http://api.service.com/v1.0/)
        if (versionedRequest.isRequestForRoot() || versionedRequest.requestMatchesVersionMapping()) {
            final JAXBElement versionElement = VERSIONING_OBJECT_FACTORY.createVersion(new VersionChoiceFactory(targetOriginService.getMapping()).create());

            transformer.transform(versionElement, getPreferredMediaRange(versionedRequest.getRequest()), filterDirector.getResponseOutputStream());

            filterDirector.setResponseStatusCode(HttpServletResponse.SC_OK);
            filterDirector.setFilterAction(FilterAction.RETURN);
        } else {
            RouteDestination dest = filterDirector.addDestination(targetOriginService.getOriginServiceHost(), versionedRequest.asInternalURI(), (float) VERSIONING_DEFAULT_QUALITY);
            dest.setContextRemoved(versionedRequest.getMapping().getId());
            filterDirector.setFilterAction(FilterAction.PASS);
        }
    }

    private void writeMultipleChoices(FilterDirector filterDirector, HttpServletRequestWrapper request) {
        filterDirector.setResponseStatusCode(HttpServletResponse.SC_MULTIPLE_CHOICES);
        filterDirector.setFilterAction(FilterAction.RETURN);

        final VersionChoiceList versionChoiceList = configurationData.versionChoicesAsList(request);
        JAXBElement<VersionChoiceList> versionChoiceListElement = VERSIONING_OBJECT_FACTORY.createChoices(versionChoiceList);

        transformer.transform(versionChoiceListElement, getPreferredMediaRange(request), filterDirector.getResponseOutputStream());
    }

    private MediaType getPreferredMediaRange(HttpServletRequestWrapper request) {
        return RequestMediaRangeInterrogator.interrogate(request.getRequestURI(), request.getPreferredSplittableHeaders(CommonHttpHeader.ACCEPT.toString())).get(0);
    }
}