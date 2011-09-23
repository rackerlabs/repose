package com.rackspace.papi.components.cnorm;

import com.rackspace.papi.commons.config.manager.LockedConfigurationUpdater;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import com.rackspace.papi.components.cnorm.normalizer.HeaderNormalizer;
import com.rackspace.papi.components.cnorm.normalizer.MediaTypeNormalizer;
import com.rackspace.papi.components.normalization.config.ContentNormalizationConfig;
import com.rackspace.papi.components.normalization.config.HeaderFilterList;
import com.rackspace.papi.components.normalization.config.MediaTypeList;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

public class ContentNormalizationHandler extends AbstractFilterLogicHandler {

    public final UpdateListener<ContentNormalizationConfig> contentNormalizationConfigurationListener;
    private final KeyedStackLock configurationLock;
    private final Object readKey, updateKey;
    private HeaderNormalizer headerNormalizer;
    private MediaTypeNormalizer mediaTypeNormalizer;

    public ContentNormalizationHandler() {
        configurationLock = new KeyedStackLock();

        readKey = new Object();
        updateKey = new Object();
        
        contentNormalizationConfigurationListener = new LockedConfigurationUpdater<ContentNormalizationConfig>(configurationLock, updateKey) {

            @Override
            protected void onConfigurationUpdated(ContentNormalizationConfig configurationObject) {
                final HeaderFilterList headerList = configurationObject.getHeaderFilters();
                final MediaTypeList mediaTypeList = configurationObject.getMediaTypes();
                
                if (headerList != null) {
                    final boolean isBlacklist = headerList.getBlacklist() != null;
                    headerNormalizer = new HeaderNormalizer(headerList, isBlacklist);
                }
                
                if (mediaTypeList != null) {
                    mediaTypeNormalizer = new MediaTypeNormalizer(mediaTypeList.getMediaType());
                }
            }
        };
    }

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);
        
        configurationLock.lock(readKey);
        
        try {
            headerNormalizer.normalizeHeaders(request, myDirector);
            mediaTypeNormalizer.normalizeContentMediaType(request, myDirector);
        } finally {
            configurationLock.unlock(readKey);
        }
        
        return myDirector;
    }
}
