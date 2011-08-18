package com.rackspace.papi.components.cnorm;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.cnorm.headers.HeaderNormalizer;
import com.rackspace.papi.components.normalization.config.ContentNormalizationConfiguration;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

public class ContentNormalizationHandler extends AbstractFilterLogicHandler {

    private HeaderNormalizer headerNormalizer;

    private final UpdateListener<ContentNormalizationConfiguration> contentNormalizationConfigurationListener = new UpdateListener<ContentNormalizationConfiguration>() {

        @Override
        public void configurationUpdated(ContentNormalizationConfiguration config) {
            final boolean isBlacklist = config.getHttpHeaderBlacklist() != null;
            
            updateHeaderNormalizer(new HeaderNormalizer(isBlacklist ? config.getHttpHeaderBlacklist().getHeader() : config.getHttpHeaderWhitelist().getHeader(), isBlacklist));
        }
    };

    public UpdateListener<ContentNormalizationConfiguration> getContentNormalizationConfigurationListener() {
        return contentNormalizationConfigurationListener;
    }
    
    private synchronized void updateHeaderNormalizer(HeaderNormalizer newNormalizer) {
        headerNormalizer = newNormalizer;
    }

    private synchronized FilterDirector normalizeHeaders(MutableHttpServletRequest request) {
        final FilterDirector myDirector = new FilterDirectorImpl();

        headerNormalizer.normalizeHeaders(request, myDirector);

        return myDirector;
    }
    
    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        return normalizeHeaders(request);
    }
}
