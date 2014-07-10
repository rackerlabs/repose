package com.rackspace.papi.service.headers.request;

import com.google.common.base.Optional;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.headers.common.ViaHeaderBuilder;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.HealthCheckServiceHelper;
import com.rackspace.papi.service.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

@Component
public class RequestHeaderServiceImpl implements RequestHeaderService, ServletContextAware {
    public static final Logger LOG = LoggerFactory.getLogger(RequestHeaderServiceImpl.class);
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";

    private final SystemModelListener systemModelListener = new SystemModelListener();
    private final ContainerConfigurationListener configurationListener = new ContainerConfigurationListener();
    private final ConfigurationService configurationService;
    private final HealthCheckService healthCheckService;

    private String reposeVersion = "";
    private String viaReceivedBy = "";
    private String hostname = "Repose";
    private ServicePorts ports;
    private ServletContext servletContext;
    private ViaHeaderBuilder viaHeaderBuilder;
    private HealthCheckServiceHelper healthCheckServiceHelper;

    @Autowired
    public RequestHeaderServiceImpl(ConfigurationService configurationService, HealthCheckService healthCheckService) {
        this.configurationService = configurationService;
        this.healthCheckService = healthCheckService;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        ports = ServletContextHelper.getInstance(servletContext).getServerPorts();
        reposeVersion = ServletContextHelper.getInstance(servletContext).getPowerApiContext().getReposeVersion();

        String healthCheckUid = healthCheckService.register(RequestHeaderServiceImpl.class);
        healthCheckServiceHelper = new HealthCheckServiceHelper(healthCheckService, LOG, healthCheckUid);

        configurationService.subscribeTo("container.cfg.xml", configurationListener, ContainerConfiguration.class);
        configurationService.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom("container.cfg.xml", configurationListener);
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public synchronized void updateConfig(ViaHeaderBuilder viaHeaderBuilder) {
        this.viaHeaderBuilder = viaHeaderBuilder;
    }

    @Override
    public void setXForwardedFor(MutableHttpServletRequest request) {
        request.addHeader(CommonHttpHeader.X_FORWARDED_FOR.toString(), request.getRemoteAddr());
    }

    @Override
    public void setVia(MutableHttpServletRequest request) {
        final String via = viaHeaderBuilder.buildVia(request);
        request.addHeader(CommonHttpHeader.VIA.toString(), via);
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
                viaReceivedBy = configurationObject.getDeploymentConfig().getVia();

                final ViaRequestHeaderBuilder viaBuilder = new ViaRequestHeaderBuilder(reposeVersion, viaReceivedBy, hostname);
                updateConfig(viaBuilder);
            }
            isInitialized = true;

        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    /**
     * Listens for updates to the system-model.cfg.xml file which holds the
     * hostname.
     */
    private class SystemModelListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel systemModel) {

            final SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);
            Optional<Node> ln = interrogator.getLocalNode(systemModel);

            if (ln.isPresent()) {
                hostname = ln.get().getHostname();

                final ViaRequestHeaderBuilder viaBuilder = new ViaRequestHeaderBuilder(reposeVersion, viaReceivedBy, hostname);
                updateConfig(viaBuilder);
                isInitialized = true;

                healthCheckServiceHelper.resolveIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT);
            } else {
                LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
                healthCheckServiceHelper.reportIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT, "Unable to identify the " +
                        "local host in the system model - please check your system-model.cfg.xml", Severity.BROKEN);
            }
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
