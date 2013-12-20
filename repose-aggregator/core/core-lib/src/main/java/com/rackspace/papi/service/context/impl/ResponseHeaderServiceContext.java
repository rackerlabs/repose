package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.headers.response.LocationHeaderBuilder;
import com.rackspace.papi.service.headers.response.ResponseHeaderService;
import com.rackspace.papi.service.headers.response.ViaResponseHeaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;

@Component("responseHeaderServiceContext")
public class ResponseHeaderServiceContext implements ServiceContext<ResponseHeaderService> {

    public static final String SERVICE_NAME = "powerapi:/services/response_header";
    private final ResponseHeaderService responseHeaderService;
    private final ServiceRegistry registry;
    private final ConfigurationService configurationManager;
    private final ContainerConfigurationListener configurationListener;
    private String reposeVersion = "";

    @Autowired
    public ResponseHeaderServiceContext(@Qualifier("responseHeaderService") ResponseHeaderService responseHeaderService,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("configurationManager") ConfigurationService configurationManager) {
        this.responseHeaderService = responseHeaderService;
        this.registry = registry;
        this.configurationManager = configurationManager;
        this.configurationListener = new ContainerConfigurationListener();
    }

    public void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public ResponseHeaderService getService() {
        return responseHeaderService;
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        reposeVersion = ServletContextHelper.getInstance(servletContextEvent.getServletContext()).getPowerApiContext().getReposeVersion();
        configurationManager.subscribeTo("container.cfg.xml", configurationListener, ContainerConfiguration.class);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
    }

    /**
     * Listens for updates to the container.cfg.xml file which holds the via
     * header receivedBy value.
     */
    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {

            if (configurationObject.getDeploymentConfig() != null) {
                final String viaReceivedBy = configurationObject.getDeploymentConfig().getVia();

                final ViaResponseHeaderBuilder viaBuilder = new ViaResponseHeaderBuilder(reposeVersion, viaReceivedBy);
                final LocationHeaderBuilder locationBuilder = new LocationHeaderBuilder();
                responseHeaderService.updateConfig(viaBuilder, locationBuilder);
            }
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
