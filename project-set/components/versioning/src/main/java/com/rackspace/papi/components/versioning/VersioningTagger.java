package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.util.http.HttpRequestInfo;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.servlet.RequestMediaRangeInterrogator;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.domain.ConfigurationData;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import java.util.Map;

/**
 *
 * @author jhopper
 */
public class VersioningTagger {
    private final VersioningTaggerHelper helper;

    public VersioningTagger(Map<String, ServiceVersionMapping> configuredMappings,
                            Map<String, Host> configuredHosts, String serviceRootHref) {
        ConfigurationData configData = new ConfigurationData(serviceRootHref, configuredHosts, configuredMappings);
        this.helper = new VersioningTaggerHelper(configData);
    }

    public FilterDirector handle(HttpRequestInfo requestInfo) {
        final MediaRange mediaRange = RequestMediaRangeInterrogator.interrogate(requestInfo.getUri(), requestInfo.getAcceptHeader());
        final FilterDirector myDirector = buildResponse(requestInfo, mediaRange.getMediaType());

        //If the delegated stats is not set then we should continue to process the request
        if (myDirector.getFilterAction() == FilterAction.NOT_SET) {
            final Host origin = helper.getOriginToRouteTo(requestInfo);

            if (origin != null) {
                /*
                STANDARD
                 */
                myDirector.requestHeaderManager().putHeader(PowerApiHeader.ORIGIN_DESTINATION.headerKey(), origin.getHref());
                myDirector.setFilterAction(FilterAction.PASS);
            } else {
                /*
                MULTIPLE_CHOICE
                 */
                return helper.returnMultipleChoices(requestInfo, mediaRange.getMediaType());
            }
        }

        return myDirector;
    }

    private FilterDirector buildResponse(HttpRequestInfo requestInfo, MediaType mediaType) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        String response;

        response = helper.transformVersioningInformationRequest(requestInfo, mediaType);

        if (response != null) {
            myDirector.setFilterAction(FilterAction.RETURN);
            myDirector.setResponseStatus(HttpStatusCode.OK);

            myDirector.getResponseWriter().write(response);
        }

        return myDirector;
    }
}
