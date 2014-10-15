package org.openrepose.core.services.context.impl;

import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.context.ServiceContext;
import org.openrepose.services.datastore.DatastoreService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;

@Component("datastoreServiceContext")
public class DatastoreServiceContext implements ServiceContext<DatastoreService> {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DatastoreServiceContext.class);
    public static final String SERVICE_NAME = "powerapi:/datastore/service";

    private final DatastoreService datastoreService;
    private final ServiceRegistry registry;
    private ReposeInstanceInfo instanceInfo;
    @Autowired
    public DatastoreServiceContext(
            @Qualifier("datastoreService") DatastoreService datastoreService,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("reposeInstanceInfo") ReposeInstanceInfo instanceInfo) {
        this.datastoreService = datastoreService;
        this.registry = registry;
        this.instanceInfo = instanceInfo;
    }

    public void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public DatastoreService getService() {
        return datastoreService;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Destroying datastore service context");
        datastoreService.shutdown();
     }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        register();
    }
}
