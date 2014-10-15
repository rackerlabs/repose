package org.openrepose.filters.cnorm;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.filters.cnorm.config.ContentNormalizationConfig;
import org.openrepose.filters.cnorm.config.HeaderFilterList;
import org.openrepose.filters.cnorm.config.MediaTypeList;
import org.openrepose.filters.cnorm.normalizer.HeaderNormalizer;
import org.openrepose.filters.cnorm.normalizer.MediaTypeNormalizer;

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
