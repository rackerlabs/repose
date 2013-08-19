package com.rackspace.papi.components.header.translation;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.header.translation.config.Header;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderTranslationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderTranslationHandler> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(HeaderTranslationHandlerFactory.class);
    private List<Header> sourceHeaders;

    @Override
    protected HeaderTranslationHandler buildHandler() {
        return null;
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(Header.class, new HeaderTranslationConfigurationListener());
            }
        };
    }

    private class HeaderTranslationConfigurationListener implements Override<Override> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(Header header) {
            sourceHeaders = header.getOriginalName().getHeader();
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
