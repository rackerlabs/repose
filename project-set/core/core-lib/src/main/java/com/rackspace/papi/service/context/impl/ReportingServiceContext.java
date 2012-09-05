package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.DestinationList;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.reporting.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import java.util.ArrayList;
import java.util.List;

@Component("reportingServiceContext")
public class ReportingServiceContext implements ServiceContext<ReportingService> {

    public static final String SERVICE_NAME = "powerapi:/services/reporting";
    public static final int REFRESH_SECONDS = 15;
    private final SystemModelListener systemModelListener;
    private final ConfigurationService configurationManager;
    private final ServiceRegistry registry;
    private final ReportingService reportingService;

    @Autowired
    public ReportingServiceContext(@Qualifier("serviceRegistry") ServiceRegistry registry,
                                   @Qualifier("configurationManager") ConfigurationService configurationManager,
                                   @Qualifier("reportingService") ReportingService reportingService) {
        this.systemModelListener = new SystemModelListener();
        this.registry = registry;
        this.configurationManager = configurationManager;
        this.reportingService = reportingService;        
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
    public ReportingService getService() {
        return reportingService;
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        configurationManager.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
    }

    /**
     * Listens for updates to the system-model.cfg.xml file which holds the destination ids.
     */
    private class SystemModelListener implements UpdateListener<SystemModel> {

        @Override
        public void configurationUpdated(SystemModel systemModel) {

            if (systemModel.getReposeCluster() != null && systemModel.getReposeCluster().get(0) != null) {

                final DestinationList destinations = systemModel.getReposeCluster().get(0).getDestinations();
                final List<String> destinationIds = new ArrayList<String>();

                for (DestinationEndpoint endpoint : destinations.getEndpoint()) {
                    destinationIds.add(endpoint.getId());
                }

                reportingService.updateConfiguration(destinationIds, REFRESH_SECONDS);
            }
        }
    }
}
