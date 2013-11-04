package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.authclient.akka.AkkaAuthenticationClient;
import com.rackspace.papi.service.context.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContextEvent;

/**
 * Manages the {@link com.rackspace.papi.service.authclient.akka.AkkaAuthenticationClient}
 */

public class AkkaAuthenticationServiceContext implements ServiceContext<AkkaAuthenticationClient> {
    private static final Logger LOG = LoggerFactory.getLogger(AkkaAuthenticationServiceContext.class);

    public static final String SERVICE_NAME = "powerapi:/services/AkkaAuthenticationClient";

    private final AkkaAuthenticationClient akkaAuthenticationClientService;
    private final ServiceRegistry registry;


    @Autowired
    public AkkaAuthenticationServiceContext(ServiceRegistry registry,
                              AkkaAuthenticationClient akkaAuthenticationClientService) {
        this.registry = registry;
        this.akkaAuthenticationClientService = akkaAuthenticationClientService;

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
    public AkkaAuthenticationClient getService() {
        return akkaAuthenticationClientService;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.debug("Initializing context for Akka Authentication Service");
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.debug("Destroying context for Akka Authentication Service");
        akkaAuthenticationClientService.shutdown();

    }

}