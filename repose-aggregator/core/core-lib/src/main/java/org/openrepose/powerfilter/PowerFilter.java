package org.openrepose.powerfilter;

import com.google.common.base.Optional;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.http.HttpStatusCode;
import org.openrepose.commons.utils.servlet.http.HttpServletHelper;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.ResponseCode;
import org.openrepose.core.filter.SystemModelInterrogator;
import org.openrepose.powerfilter.filtercontext.FilterContext;
import org.openrepose.powerfilter.filtercontext.FilterContextFactory;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.container.ContainerConfigurationService;
import org.openrepose.core.services.deploy.ApplicationDeploymentEvent;
import org.openrepose.core.services.event.PowerFilterEvent;
import org.openrepose.core.services.event.common.Event;
import org.openrepose.core.services.event.common.EventListener;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.services.headers.response.ResponseHeaderService;
import org.openrepose.core.services.reporting.ReportingService;
import org.openrepose.core.services.reporting.metrics.MeterByCategory;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.rms.ResponseMessageService;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.Destination;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class implements the Filter API and is managed by the servlet container.  This filter then loads
 * and runs the FilterChain which contains the individual filter instances listed in the system-model.cfg.xml.
 * <p/>
 * This class current instruments the response codes coming from Repose.
 * <p/>
 * TODO: this must have the application context wired into it....
 * THen the application context must be handed down into things that need it to build filters.
 * TODO: this also needs to check the properties to make sure they exist before we start up like the empty servlet used
 * to do.
 */
@Named("powerFilter")
public class PowerFilter extends DelegatingFilterProxy {
    private static final Logger LOG = LoggerFactory.getLogger(PowerFilter.class);
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";
    public static final String APPLICATION_DEPLOYMENT_HEALTH_REPORT = "ApplicationDeploymentError";

    private final Object configurationLock = new Object();
    private final EventListener<ApplicationDeploymentEvent, List<String>> applicationDeploymentListener;
    private final UpdateListener<SystemModel> systemModelConfigurationListener;
    private final HealthCheckService healthCheckService;
    private final ContainerConfigurationService containerConfigurationService;
    private final ResponseMessageService responseMessageService;
    private final EventService eventService;
    private final FilterContextFactory filterContextFactory;

    private PowerFilterChainBuilder powerFilterChainBuilder;
    private final AtomicReference<SystemModel> currentSystemModel = new AtomicReference<>();

    private ReportingService reportingService;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private MeterByCategory mbcResponseCodes;
    private ResponseHeaderService responseHeaderService;

    private final String nodeId;
    private final String clusterId;
    private final String configRoot;
    private final ConfigurationService configurationService;
    private final MetricsService metricsService;

    @Inject
    public PowerFilter(
            @Value(ReposeSpringProperties.NODE.CLUSTER_ID)String clusterId,
            @Value(ReposeSpringProperties.NODE.NODE_ID)String nodeId,
            @Value(ReposeSpringProperties.CORE.CONFIG_ROOT)String configRoot,
            PowerFilterChainBuilder powerFilterChainBuilder,
            ReportingService reportingService,
            HealthCheckService healthCheckService,
            ResponseHeaderService responseHeaderService,
            ConfigurationService configurationService,
            EventService eventService,
            MetricsService metricsService,
            ContainerConfigurationService containerConfigurationService,
            ResponseMessageService responseMessageService,
            FilterContextFactory filterContextFactory
    ) {
        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.configRoot = configRoot;
        this.configurationService = configurationService;
        this.metricsService = metricsService;

        // Set up the configuration listeners
        systemModelConfigurationListener = new SystemModelConfigListener();
        applicationDeploymentListener = new ApplicationDeploymentEventListener();

        this.powerFilterChainBuilder = powerFilterChainBuilder;
        this.responseHeaderService = responseHeaderService;
        this.reportingService = reportingService;
        this.containerConfigurationService = containerConfigurationService;
        this.responseMessageService = responseMessageService;
        this.eventService = eventService;
        this.filterContextFactory = filterContextFactory;

        this.healthCheckService = healthCheckService;

        healthCheckServiceProxy = healthCheckService.register();
        mbcResponseCodes = metricsService.newMeterByCategory(ResponseCode.class, "Repose", "Response Code", TimeUnit.SECONDS);

    }

    private class ApplicationDeploymentEventListener implements EventListener<ApplicationDeploymentEvent, List<String>> {

        @Override
        public void onEvent(Event<ApplicationDeploymentEvent, List<String>> e) {
            LOG.info("Application collection has been modified. Application that changed: " + e.payload());

            // Using a set instead of a list to have a deployment health report if there are multiple artifacts with the same name
            Set<String> uniqueArtifacts = new HashSet<>();
            try {
                for (String artifactName : e.payload()) {
                    uniqueArtifacts.add(artifactName);
                }
                healthCheckServiceProxy.resolveIssue(APPLICATION_DEPLOYMENT_HEALTH_REPORT);
            } catch (IllegalArgumentException exception) {
                healthCheckServiceProxy.reportIssue(APPLICATION_DEPLOYMENT_HEALTH_REPORT, "Please review your artifacts directory, multiple " +
                        "versions of the same artifact exist!", Severity.BROKEN);
                LOG.error("Please review your artifacts directory, multiple versions of same artifact exists.");
            }

            configurationHeartbeat();
        }
    }

    private class SystemModelConfigListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            SystemModel previousSystemModel = currentSystemModel.getAndSet(configurationObject);
            if (previousSystemModel == null) {
                eventService.newEvent(PowerFilterEvent.POWER_FILTER_CONFIGURED, System.currentTimeMillis());
            }

            configurationHeartbeat();
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    /**
     * Triggered each time the event service triggers an app deploy and when the system model is updated.
     */
    private void configurationHeartbeat() {
        if (currentSystemModel.get() != null) {
            synchronized (configurationLock) {
                SystemModelInterrogator interrogator = new SystemModelInterrogator(clusterId, nodeId);
                SystemModel systemModel = currentSystemModel.get();

                Optional<Node> localNode = interrogator.getLocalNode(systemModel);
                Optional<ReposeCluster> localCluster = interrogator.getLocalCluster(systemModel);
                Optional<Destination> defaultDestination = interrogator.getDefaultDestination(systemModel);

                if (localNode.isPresent() && localCluster.isPresent() && defaultDestination.isPresent()) {
                    ReposeCluster serviceDomain = localCluster.get();
                    Destination defaultDst = defaultDestination.get();

                    healthCheckServiceProxy.resolveIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT);
                    try {
                        //Use the FilterContextFactory to get us a new filter chain
                        final List<FilterContext> newFilterChain = filterContextFactory.buildFilterContexts(getFilterConfig(), localCluster.get().getFilters().getFilter());

                        //Reinitialize the power filter chain builder with new settings.
                        powerFilterChainBuilder.initialize(serviceDomain, localNode.get(), newFilterChain, getFilterConfig().getServletContext(), defaultDst.getId());
                    } catch (FilterInitializationException fie) {
                        LOG.error("Unable to create new filter chain", fie);
                    } catch (PowerFilterChainException e) {
                        LOG.error("Unable to initialize filter chain builder", e);
                    }

                    LOG.info("Repose ready");
                } else {
                    LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
                    healthCheckServiceProxy.reportIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT, "Unable to identify the " +
                            "local host in the system model - please check your system-model.cfg.xml", Severity.BROKEN);
                }
            }
        }
    }

    @Override
    public void initFilterBean() {
        eventService.listen(applicationDeploymentListener, ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED);

        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationService.subscribeTo("", "system-model.cfg.xml", xsdURL, systemModelConfigurationListener, SystemModel.class);
    }

    @Override
    public void destroy() {

        eventService.squelch(applicationDeploymentListener, ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED);
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelConfigurationListener);

        //TODO: wat
        synchronized (configurationLock) {
            if (powerFilterChainBuilder != null) {
                powerFilterChainBuilder.destroy();
            }
        }
    }

    private PowerFilterChain getRequestFilterChain(MutableHttpServletResponse mutableHttpResponse, FilterChain chain) throws ServletException {
        PowerFilterChain requestFilterChain = null;
        try {
            synchronized (configurationLock) {
                if (!healthCheckService.isHealthy()) {
                    LOG.warn("Repose is not healthy enough to serve requests!");
                    mutableHttpResponse.sendError(HttpStatusCode.SERVICE_UNAVAIL.intValue(), "Currently unable to serve requests");
                } else {
                    requestFilterChain = powerFilterChainBuilder.newPowerFilterChain(chain);
                }
            }
        } catch (PowerFilterChainException ex) {
            LOG.warn("Error creating filter chain", ex);
            mutableHttpResponse.sendError(HttpStatusCode.SERVICE_UNAVAIL.intValue(), "Error creating filter chain");
            mutableHttpResponse.setLastException(ex);
        }

        return requestFilterChain;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final long startTime = System.currentTimeMillis();
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);
        long streamLimit = containerConfigurationService.getContentBodyReadLimit();

        final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request, streamLimit);
        final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, (HttpServletResponse) response);

        try {
            new URI(mutableHttpRequest.getRequestURI());
            final PowerFilterChain requestFilterChain = getRequestFilterChain(mutableHttpResponse, chain);
            if (requestFilterChain != null) {
                requestFilterChain.startFilterChain(mutableHttpRequest, mutableHttpResponse);
            }
        } catch (URISyntaxException use) {
            LOG.debug("Invalid URI requested: {}", mutableHttpRequest.getRequestURI());
            mutableHttpResponse.sendError(HttpStatusCode.BAD_REQUEST.intValue(), "Error processing request");
            mutableHttpResponse.setLastException(use);
        } catch (Exception ex) {
            LOG.error("Exception encountered while processing filter chain. Reason: " + ex.getMessage(), ex);
            mutableHttpResponse.sendError(HttpStatusCode.BAD_GATEWAY.intValue(), "Error processing request");
            mutableHttpResponse.setLastException(ex);
        } finally {
            // In the case where we pass/route the request, there is a chance that
            // the response will be committed by an underlying service, outside of repose
            if (!mutableHttpResponse.isCommitted()) {
                responseMessageService.handle(mutableHttpRequest, mutableHttpResponse);
                responseHeaderService.setVia(mutableHttpRequest, mutableHttpResponse);
            }

            try {
                mutableHttpResponse.writeHeadersToResponse();
                mutableHttpResponse.commitBufferToServletOutputStream();
            } catch (IOException ex) {
                LOG.error("Error committing output stream", ex);
            }
            final long stopTime = System.currentTimeMillis();

            markResponseCodeHelper(mbcResponseCodes, ((HttpServletResponse) response).getStatus(), LOG, null);

            reportingService.incrementReposeStatusCodeCount(((HttpServletResponse) response).getStatus(), stopTime - startTime);
        }
    }

    public static void markResponseCodeHelper(MeterByCategory mbc, int responseCode, Logger log, String logPrefix) {
        if (mbc == null) {
            return;
        }

        int code = responseCode / 100;

        if (code == 2) {
            mbc.mark("2XX");
        } else if (code == 3) {
            mbc.mark("3XX");
        } else if (code == 4) {
            mbc.mark("4XX");
        } else if (code == 5) {
            mbc.mark("5XX");
        } else {
            log.error((logPrefix != null ? logPrefix + ":  " : "") + "Encountered invalid response code: " + responseCode);
        }
    }
}
