package com.rackspace.papi.jmx;

import com.google.common.base.Optional;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy;
import com.rackspace.papi.service.healthcheck.Severity;
import org.openrepose.core.service.config.ConfigurationService;
import org.openrepose.core.service.config.manager.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * This class arrived out of the need to detach the ConfigurationInformation from the Configuration Service
 * Spring detected a cyclic dependency.
 *
 * TODO: fix this poor thing
 * This is probably not the right way to do this stuff, a better way needs to be done when the ConfigurationService is
 * redone properly.
 *
 * TODO: this probably doesn't actually need a SystemModelInterrogator, it just wants a list of what's local
 * There's probably a much simpler way to determine what filters this node needs to run... Probably based off
 * ClusterID which should be unique with the new way of doing things.
 *
 */
@Named
public class FilterListProvider {
    
    private static final Logger LOG = LoggerFactory.getLogger(FilterListProvider.class);

    private final ConfigurationService configurationService;
    private final ConfigurationInformation configurationInformation;
    private final HealthCheckService healthCheckService;
    private final SystemModelInterrogator systemModelInterrogator;

    private SystemModelListener systemModelListener;
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";
    private HealthCheckServiceProxy healthCheckServiceProxy;

    @Inject
    public FilterListProvider(SystemModelInterrogator systemModelInterrogator,
                              ConfigurationService configurationService,
                              ConfigurationInformation configurationInformation,
                              HealthCheckService healthCheckService) {
        this.configurationService = configurationService;
        this.configurationInformation = configurationInformation;
        this.systemModelInterrogator = systemModelInterrogator;
        this.healthCheckService = healthCheckService;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        systemModelListener = new SystemModelListener();
        configurationService.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);

        healthCheckServiceProxy = healthCheckService.register();
    }

    @PreDestroy
    public void destroy() {
        healthCheckServiceProxy.deregister();
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
    }


    private class SystemModelListener implements UpdateListener<SystemModel> {

        private boolean initialized = false;

        /**
         * Whenever the system-model is updated get a list of all the filters that are configured.
         * @param systemModel
         */
        @Override
        public void configurationUpdated(SystemModel systemModel) {
            LOG.info("System model updated");
            initialized = false;

            List<FilterInformation> filterList = configurationInformation.getFilterList();

            Optional<ReposeCluster> cluster = systemModelInterrogator.getLocalCluster(systemModel);

            if (cluster.isPresent()) {
                synchronized (filterList) {
                    filterList.clear();

                    if (cluster.get().getFilters() != null && cluster.get().getFilters().getFilter() != null) {
                        for (Filter filter : cluster.get().getFilters().getFilter()) {
                            filterList.add(new FilterInformation(filter.getId(), filter.getName(), filter.getUriRegex(),
                                    filter.getConfiguration(), false));
                        }
                    }
                }

                initialized = true;

                healthCheckServiceProxy.resolveIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT);
            } else {
                LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
                healthCheckServiceProxy.reportIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT, "Unable to identify the " +
                        "local host in the system model - please check your system-model.cfg.xml", Severity.BROKEN);
            }
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }

}
