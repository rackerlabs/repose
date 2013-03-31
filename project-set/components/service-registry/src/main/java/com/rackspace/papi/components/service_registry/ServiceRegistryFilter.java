package com.rackspace.papi.components.service_registry;

import com.rackspacecloud.client.service_registry.Client;
import com.rackspacecloud.client.service_registry.HeartBeater;
import com.rackspacecloud.client.service_registry.Region;
import com.rackspacecloud.client.service_registry.ServiceCreateResponse;

import com.rackspace.papi.components.service_registry.config.ServiceRegistryConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import sun.util.logging.resources.logging;

public class ServiceRegistryFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRegistryFilter.class);
    private static final String DEFAULT_CONFIG = "service-registry.cfg.xml";

    private String config;

    private Client rsrClient = null;
    private HeartBeater heartbeater = null;
    private String serviceId = null;

    @Override
    public void destroy() {
        if (this.heartbeater != null) {
            this.heartbeater.stop();
            this.heartbeater = null;
        }

        // destroy is called when a repose instance is gracefully shut down.
        // We explicitly remove service from the Service Registry so we can
        // distinguish between a normal shutdown which doesn't require any action
        // on the user side (service.remove event) and an unexpected shutdown
        // (service.timeout) event.
        if (this.serviceId != null) {
            try {
                this.rsrClient.getServicesClient().delete(this.serviceId);
            }
            catch (Exception ex) {
                LOG.info("Failed to delete a service: " + ex.toString());
            }

            this.serviceId = null;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // Pass through
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);

        String username, apiKey, region;

        // Auth Config
        username = config.getUsername();
        apiKey = config.getApiKey();
        region = config.getRegion();

        // Service config
        String serviceId = config.getServiceId();
        Integer heartbeatTimeout = config.getHeartbeatTimeout();
        ArrayList<String> tags = config.getTags();
        HashMap<String, String> metadata = new HashMap<String, String>();

        this.rsrClient = new Client(username, apiKey, region);

        LOG.info("Registering service in the registry: " + serviceId);

        try {
            ServiceCreateResponse response = this.rsrClient.getServicesClient().create(serviceId, heartbeatTimeout, tags, metadata);
        }
        catch (Exception ex) {
            LOG.info("Failed to create a service: " + ex.toString());
            return;
        }

        this.heartbeater = response.getHeartbeater();
        this.heartbeater.start();
    }
}