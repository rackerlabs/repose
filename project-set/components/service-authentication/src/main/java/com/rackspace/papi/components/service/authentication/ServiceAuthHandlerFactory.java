package com.rackspace.papi.components.service.authentication;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ServiceAuthHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ServiceAuthHandler> {

    private ServiceAuthenticationConfig config;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ServiceAuthHandler.class);
    private String basicAuthCredentials;

    public ServiceAuthHandlerFactory() {
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(ServiceAuthenticationConfig.class, new ClientIpIdentityConfigurationListener());
            }
        };
    }

    private class ClientIpIdentityConfigurationListener implements UpdateListener<ServiceAuthenticationConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ServiceAuthenticationConfig configurationObject) {
            config = configurationObject;
            basicAuthCredentials = "";

            if (config != null && config.getCredentials() != null) {
                basicAuthCredentials = buildCredentials();
            }

            isInitialized = true;
        }

        private String buildCredentials() {
            StringBuilder preHash = new StringBuilder(config.getCredentials().getUsername());
            StringBuilder postHash = new StringBuilder("Basic ");
            preHash.append(":").append(config.getCredentials().getPassword());
            try {
                postHash.append(Base64.encodeBase64String(preHash.toString().getBytes("UTF-8")).trim());
            } catch (UnsupportedEncodingException e) {
                LOG.error("Failed to update basic credentials. Reason: " + e.getMessage(), e);
            }

            return postHash.toString();
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected ServiceAuthHandler buildHandler() {

        if (!this.isInitialized()) {
            return null;
        }
        return new ServiceAuthHandler(basicAuthCredentials);
    }
}
