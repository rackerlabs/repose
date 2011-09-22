package com.rackspace.papi.components.cnorm;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.cnorm.headers.HeaderNormalizer;
import com.rackspace.papi.components.normalization.config.ContentNormalizationConfig;
import com.rackspace.papi.components.normalization.config.HeaderFilterList;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

public class ContentNormalizationHandler extends AbstractFilterLogicHandler {

    private HeaderNormalizer headerNormalizer;

    private final UpdateListener<ContentNormalizationConfig> contentNormalizationConfigurationListener = new UpdateListener<ContentNormalizationConfig>() {

        @Override
        public void configurationUpdated(ContentNormalizationConfig config) {
            final HeaderFilterList headerList = config.getHeaderList();
            
            if (headerList != null) {
                final boolean isBlacklist = headerList.getHttpHeaderBlacklist() != null;
            
                updateHeaderNormalizer(new HeaderNormalizer(isBlacklist 
                        ? headerList.getHttpHeaderBlacklist().getHeader()
                        : headerList.getHttpHeaderWhitelist().getHeader(), isBlacklist));
            }
        }
    };

    public UpdateListener<ContentNormalizationConfig> getContentNormalizationConfigurationListener() {
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
