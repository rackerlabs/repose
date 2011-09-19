package com.rackspace.papi.components.cneg;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.config.cneg.ContentNegotiation;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterDirector;

/**
 *
 * @author jhopper
 */
public class ContentNegotiationHandler extends AbstractFilterLogicHandler {

    public ContentNegotiationHandler() {
    }

    private final UpdateListener<ContentNegotiation> contentNegotiationConfigurationListener = new UpdateListener<ContentNegotiation>() {

        @Override
        public void configurationUpdated(ContentNegotiation modifiedConfig) {
            throw new UnsupportedOperationException("Not supported");
        }
    };

    public UpdateListener<ContentNegotiation> getContentNegotiationConfigurationListener() {
        return contentNegotiationConfigurationListener;
    }

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        // TODO: Add logic for content negotiation
        return null;
    }
}
