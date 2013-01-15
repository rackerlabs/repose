package com.rackspace.papi.components.cnorm;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.cnorm.normalizer.HeaderNormalizer;
import com.rackspace.papi.components.cnorm.normalizer.MediaTypeNormalizer;
import com.rackspace.papi.components.normalization.config.ContentNormalizationConfig;
import com.rackspace.papi.components.normalization.config.HeaderFilterList;
import com.rackspace.papi.components.normalization.config.MediaTypeList;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.HashMap;
import java.util.Map;

public class ContentNormalizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ContentNormalizationHandler> {

    private HeaderNormalizer headerNormalizer;
    private MediaTypeNormalizer mediaTypeNormalizer;

    public ContentNormalizationHandlerFactory() {
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(ContentNormalizationConfig.class, new ContentNormalizationConfigurationListener());
            }
        };
    }

    private class ContentNormalizationConfigurationListener implements UpdateListener<ContentNormalizationConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContentNormalizationConfig configurationObject) {
            final HeaderFilterList headerList = configurationObject.getHeaderFilters();
            final MediaTypeList mediaTypeList = configurationObject.getMediaTypes();

            if (headerList != null) {
                final boolean isBlacklist = headerList.getBlacklist() != null;
                headerNormalizer = new HeaderNormalizer(headerList, isBlacklist);
            }

            if (mediaTypeList != null) {
                mediaTypeNormalizer = new MediaTypeNormalizer(mediaTypeList.getMediaType());
            }
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected ContentNormalizationHandler buildHandler() {

        if (!this.isInitialized()) {
            return null;
        }

        return new ContentNormalizationHandler(headerNormalizer, mediaTypeNormalizer);
    }
}
