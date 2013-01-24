package com.rackspace.papi.components.identity.header;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.identity.header.config.HeaderIdentityConfig;
import com.rackspace.papi.components.identity.header.config.HttpHeader;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderIdentityHandler> {

    private List<HttpHeader> sourceHeaders;

    public HeaderIdentityHandlerFactory() {
        sourceHeaders = new ArrayList<HttpHeader>();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(HeaderIdentityConfig.class, new HeaderIdentityConfigurationListener());
            }
        };
    }

    private class HeaderIdentityConfigurationListener implements UpdateListener<HeaderIdentityConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(HeaderIdentityConfig configurationObject) {
            sourceHeaders = configurationObject.getSourceHeaders().getHeader();
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected HeaderIdentityHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new HeaderIdentityHandler(sourceHeaders);
    }
}
