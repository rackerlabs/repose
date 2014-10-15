package org.openrepose.filters.uristripper;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.filters.uristripper.config.UriStripperConfig;

import java.util.HashMap;
import java.util.Map;

public class UriStripperHandlerFactory extends AbstractConfiguredFilterHandlerFactory<UriStripperHandler> {


    int stripId;
    boolean rewriteLocation;

    protected UriStripperHandlerFactory() {
    }

    @Override
    protected UriStripperHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new UriStripperHandler(stripId,rewriteLocation);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(UriStripperConfig.class, new UriTranslationConfigurationListener());
            }
        };
    }

    private class UriTranslationConfigurationListener implements UpdateListener<UriStripperConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(UriStripperConfig config) {

            stripId = config.getTokenIndex();
            rewriteLocation = config.isRewriteLocation();
            isInitialized = true;

        }
        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
