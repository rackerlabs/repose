package com.rackspace.papi.jmx;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextAware;
import java.util.ArrayList;
import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.servlet.ServletContextEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component("reposeConfigurationInformation")
@ManagedResource(objectName = "com.rackspace.papi.jmx:type=ConfigurationInformation", description = "Repose configuration information MBean.")
public class ConfigurationInformation implements ConfigurationInformationMBean, ServletContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationInformation.class);
    private final ConfigurationService configurationService;
    private ServicePorts ports;
    private final List<FilterInformation> filterChain;
    private SystemModelListener systemModelListener;

    public static class FilterInformation {

        private final String id;
        private final String name;
        private final String regex;
        private final String configuration;

        public FilterInformation(String id, String name, String regex, String configuration) {
            this.id = id;
            this.name = name;
            this.regex = regex;
            this.configuration = configuration;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getRegex() {
            return regex;
        }

        public String getConfiguration() {
            return configuration;
        }
    }

    private static class SystemModelListener implements UpdateListener<SystemModel> {

        private boolean initialized = false;
        private final List<FilterInformation> filters;
        private final ServicePorts ports;

        SystemModelListener(List<FilterInformation> filters, ServicePorts ports) {
            this.filters = filters;
            this.ports = ports;
        }

        @Override
        public void configurationUpdated(SystemModel systemModel) {
            LOG.info("System model updated");
            initialized = false;

            SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);
            ReposeCluster cluster = interrogator.getLocalServiceDomain(systemModel);

            synchronized (filters) {
                filters.clear();

                for (Filter filter : cluster.getFilters().getFilter()) {
                    filters.add(new FilterInformation(filter.getId(), filter.getName(), filter.getUriRegex(), filter.getConfiguration()));
                }
            }

            initialized = true;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }

    @Autowired
    public ConfigurationInformation(@Qualifier("configurationManager") ConfigurationService configurationService, @Qualifier("servicePorts") ServicePorts ports) {
        filterChain = new ArrayList<FilterInformation>();
        this.configurationService = configurationService;
        this.ports = ports;
    }

    @Override
    @ManagedOperation
    public List<CompositeData> getFilterChain() throws OpenDataException {
        List<CompositeData> list = new ArrayList<CompositeData>();
        synchronized (filterChain) {
            for (FilterInformation filter: filterChain) {
                list.add(new ConfigurationInformationCompositeDataBuilder(filter).toCompositeData());
            }
        }
        
        return list;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        systemModelListener = new SystemModelListener(filterChain, ports);
        configurationService.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
        systemModelListener = null;
    }
}
