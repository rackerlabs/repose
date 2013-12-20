package com.rackspace.papi.components.header.translation;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.header.translation.config.Header;
import com.rackspace.papi.components.header.translation.config.HeaderTranslationType;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderTranslationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderTranslationHandler> {

    private List<Header> sourceHeaders;

    protected HeaderTranslationHandlerFactory() {
        sourceHeaders = new ArrayList<Header>();
    }

    @Override
    protected HeaderTranslationHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new HeaderTranslationHandler(sourceHeaders);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(HeaderTranslationType.class, new HeaderTranslationConfigurationListener());
            }
        };
    }

    private class HeaderTranslationConfigurationListener implements UpdateListener<HeaderTranslationType> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(HeaderTranslationType headerTranslationTypeConfigObject) {
            sourceHeaders = headerTranslationTypeConfigObject.getHeader();
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
