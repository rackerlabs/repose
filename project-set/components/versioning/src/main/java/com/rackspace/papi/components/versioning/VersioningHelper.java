package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.util.http.HttpRequestInfo;
import com.rackspace.papi.commons.util.http.HttpRequestInfoImpl;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.versioning.domain.ConfigurationData;
import com.rackspace.papi.components.versioning.domain.VersionedHostNotFoundException;
import com.rackspace.papi.components.versioning.domain.VersionedOriginService;
import com.rackspace.papi.components.versioning.domain.VersionedRequest;
import com.rackspace.papi.components.versioning.schema.ObjectFactory;
import com.rackspace.papi.components.versioning.schema.VersionChoiceList;
import com.rackspace.papi.components.versioning.util.ContentTransformer;
import com.rackspace.papi.components.versioning.util.VersionChoiceFactory;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersioningHelper {

    private static final Logger LOG = LoggerFactory.getLogger(VersioningHelper.class);
    private static final ObjectFactory VERSIONING_OBJECT_FACTORY = new ObjectFactory();
    private final ConfigurationData configurationData;
    private final ContentTransformer transformer;

    public VersioningHelper(ConfigurationData configurationData) {
        this.configurationData = configurationData;
        this.transformer = new ContentTransformer();
    }

    public FilterDirector handleRequest(HttpServletRequest request) {
        final FilterDirector filterDirector = new FilterDirectorImpl();
        final HttpRequestInfo httpRequestInfo = new HttpRequestInfoImpl(request);
        
        try {
            final VersionedOriginService targetOriginService = configurationData.getOriginServiceForRequest(httpRequestInfo);

            if (targetOriginService != null) {
                final VersionedRequest versionedRequest = new VersionedRequest(
                        httpRequestInfo, targetOriginService.getMapping(), configurationData.getServiceRootHref());

                handleVersionedRequest(versionedRequest, filterDirector, targetOriginService);
            } else {
                handleUnversionedRequest(httpRequestInfo, filterDirector);
            }
        } catch (VersionedHostNotFoundException vhnfe) {
            filterDirector.setResponseStatus(HttpStatusCode.BAD_GATEWAY);
            filterDirector.setFilterAction(FilterAction.RETURN);

            LOG.warn("Configured versioned service mapping refers to a bad pp-host-id", vhnfe);
        }

        // This is not a version we recognize - tell the client what's up
        if (filterDirector.getFilterAction() == FilterAction.NOT_SET) {
            writeMultipleChoices(filterDirector, httpRequestInfo);
        }

        return filterDirector;
    }

    private void handleUnversionedRequest(HttpRequestInfo httpRequestInfo, FilterDirector filterDirector) {
        // Is this a request to the service root to describe the available versions? (e.g. http://api.service.com/)
        if (configurationData.isRequestForVersions(httpRequestInfo)) {
            filterDirector.setResponseStatus(HttpStatusCode.OK);
            filterDirector.setFilterAction(FilterAction.RETURN);

            final JAXBElement<VersionChoiceList> versions = VERSIONING_OBJECT_FACTORY.createVersions(configurationData.versionChoicesAsList(httpRequestInfo));
            transformer.transform(versions, httpRequestInfo.getPreferedMediaRange(), filterDirector.getResponseOutputStream());
        }
    }

    private void handleVersionedRequest(VersionedRequest versionedRequest, FilterDirector filterDirector, VersionedOriginService targetOriginService) {
        // Is this a reuest to a version root we are aware of for describing it? (e.g. http://api.service.com/v1.0/)
        if (versionedRequest.isRequestForRoot() || versionedRequest.requestMatchesVersionMapping()) {
            filterDirector.setResponseStatus(HttpStatusCode.OK);
            filterDirector.setFilterAction(FilterAction.RETURN);

            final JAXBElement versionElement = VERSIONING_OBJECT_FACTORY.createVersion(new VersionChoiceFactory(targetOriginService.getMapping()).create());

            transformer.transform(versionElement, versionedRequest.getRequestInfo().getPreferedMediaRange(), filterDirector.getResponseOutputStream());
        } else {
            filterDirector.setFilterAction(FilterAction.PASS);
            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.ROUTE_DESTINATION.headerKey(), targetOriginService.getMapping().getId());

            // Set the URI to the correct, internally versioned path and pass it to the origin service
            filterDirector.setRequestUri(versionedRequest.asInternalURI());
            filterDirector.setRequestUrl(new StringBuffer(versionedRequest.asInternalURL()));
        }
    }

    private void writeMultipleChoices(FilterDirector filterDirector, HttpRequestInfo httpRequestInfo) {
        filterDirector.setResponseStatus(HttpStatusCode.MULTIPLE_CHOICES);
        filterDirector.setFilterAction(FilterAction.RETURN);

        final VersionChoiceList versionChoiceList = configurationData.versionChoicesAsList(httpRequestInfo);
        JAXBElement<VersionChoiceList> versionChoiceListElement = VERSIONING_OBJECT_FACTORY.createChoices(versionChoiceList);

        transformer.transform(versionChoiceListElement, httpRequestInfo.getPreferedMediaRange(), filterDirector.getResponseOutputStream());
    }
}
