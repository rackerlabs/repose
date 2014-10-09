package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.service.ServiceRegistry;
import org.openrepose.services.serviceclient.akka.api.AkkaServiceClient;
import com.rackspace.papi.service.context.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContextEvent;

/**
 * Manages the {@link org.openrepose.services.serviceclient.akka.api.AkkaServiceClient}
 */

public class AkkaServiceClientContext implements ServiceContext<AkkaServiceClient> {
    private static final Logger LOG = LoggerFactory.getLogger(AkkaServiceClientContext.class);

    public static final String SERVICE_NAME = "powerapi:/services/AkkaServiceClient";

    private final AkkaServiceClient akkaServiceClientService;
    private final ServiceRegistry registry;


    @Autowired
    public AkkaServiceClientContext(ServiceRegistry registry,
                                    AkkaServiceClient akkaServiceClientService) {
        this.registry = registry;
        this.akkaServiceClientService = akkaServiceClientService;

    }

    private void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public AkkaServiceClient getService() {
        return akkaServiceClientService;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.debug("Initializing context for Akka Authentication Service");
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.debug("Destroying context for Akka Authentication Service");
        akkaServiceClientService.shutdown();

    }

}
