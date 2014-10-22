package org.openrepose.core.filter;

import com.google.common.base.Optional;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.http.HttpStatusCode;
import org.openrepose.commons.utils.servlet.http.HttpServletHelper;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.ResponseCode;
import org.openrepose.core.filter.filtercontext.FilterContext;
import org.openrepose.core.filter.filtercontext.FilterContextFactory;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class implements the Filter API and is managed by the servlet container.  This filter then loads
 * and runs the FilterChain which contains the individual filter instances listed in the system-model.cfg.xml.
 * <p/>
 * This class current instruments the response codes coming from Repose.
 * <p/>
 * TODO: this must have the application context wired into it....
 * THen the application context must be handed down into things that need it to build filters.
 */
@Named("powerFilter")
public class PowerFilter extends DelegatingFilterProxy {
    private static final Logger LOG = LoggerFactory.getLogger(PowerFilter.class);
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";
    public static final String APPLICATION_DEPLOYMENT_HEALTH_REPORT = "ApplicationDeploymentError";

    private final Object internalLock = new Object();
    private final EventListener<ApplicationDeploymentEvent, List<String>> applicationDeploymentListener;
    private final UpdateListener<SystemModel> systemModelConfigurationListener;
    private final ApplicationContext applicationContext;
    private final HealthCheckService healthCheckService;
    private final ContainerConfigurationService containerConfigurationService;
    private final ResponseMessageService responseMessageService;
    private final EventService eventService;
    private final FilterContextFactory filterContextFactory;

    private boolean firstInitialization; //TODO I think this is real bad
    //Ports will come from a property in spring
    //Node ID and cluster ID will come from properties in spring
    private PowerFilterChainBuilder powerFilterChainBuilder;
    private SystemModel currentSystemModel;
    private ReposeCluster serviceDomain; //TODO: should not need this... services will be started and ready to go
    private ReportingService reportingService;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private MeterByCategory mbcResponseCodes;
    private ResponseHeaderService responseHeaderService;
    private Destination defaultDst;

    private final String nodeId;
    private final String clusterId;
    private final String configRoot;

    @Inject
    public PowerFilter(
            Environment springEnvironment,
            ApplicationContext applicationContext,
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
        firstInitialization = true; //I think this is really really super bad very much not good

        // Set up the configuration listeners
        systemModelConfigurationListener = new SystemModelConfigListener();
        applicationDeploymentListener = new ApplicationDeploymentEventListener();

        this.powerFilterChainBuilder = powerFilterChainBuilder;
        this.applicationContext = applicationContext;
        this.responseHeaderService = responseHeaderService;
        this.reportingService = reportingService;
        this.containerConfigurationService = containerConfigurationService;
        this.responseMessageService = responseMessageService;
        this.eventService = eventService;
        this.filterContextFactory = filterContextFactory;

        this.healthCheckService = healthCheckService;

        eventService.listen(applicationDeploymentListener, ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED);

        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationService.subscribeTo("", "system-model.cfg.xml", xsdURL, systemModelConfigurationListener, SystemModel.class);

        healthCheckServiceProxy = healthCheckService.register();
        mbcResponseCodes = metricsService.newMeterByCategory(ResponseCode.class, "Repose", "Response Code", TimeUnit.SECONDS);

        this.clusterId = springEnvironment.getProperty(ReposeSpringProperties.CLUSTER_ID);
        this.nodeId = springEnvironment.getProperty(ReposeSpringProperties.NODE_ID);
        this.configRoot = springEnvironment.getProperty(ReposeSpringProperties.CONFIG_ROOT);
    }

    private class ApplicationDeploymentEventListener implements EventListener<ApplicationDeploymentEvent, List<String>> {

        @Override
        public void onEvent(Event<ApplicationDeploymentEvent, List<String>> e) {
            LOG.info("Application collection has been modified. Application that changed: " + e.payload());

            List<String> uniqueArtifactList = new ArrayList<String>();

            for (String artifactName : e.payload()) {
                if (!uniqueArtifactList.contains(artifactName)) {
                    uniqueArtifactList.add(artifactName);
                } else {
                    LOG.error("Please review your artifacts directory, multiple versions of same artifact exists.");
                }
            }

            if (currentSystemModel != null) {
                SystemModelInterrogator interrogator = new SystemModelInterrogator(clusterId, nodeId);

                Optional<Node> ln = interrogator.getLocalNode(currentSystemModel);
                Optional<ReposeCluster> lc = interrogator.getLocalCluster(currentSystemModel);
                Optional<Destination> dd = interrogator.getDefaultDestination(currentSystemModel);

                if (ln.isPresent() && lc.isPresent() && dd.isPresent()) {
                    serviceDomain = lc.get();
                    defaultDst = dd.get();
                    healthCheckServiceProxy.resolveIssue(APPLICATION_DEPLOYMENT_HEALTH_REPORT);

                    final List<FilterContext> newFilterChain = filterContextFactory.buildFilterContexts(getFilterConfig(), lc.get().getFilters().getFilter());
                    updateFilterChainBuilder(newFilterChain);
                } else {
                    // Note: This should never occur! If it does, the currentSystemModel is being set to something
                    // invalid, and that should be prevented in the SystemModelConfigListener below. Resolution of
                    // this issue will only occur when the config is fixed and the application is redeployed.
                    LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
                    healthCheckServiceProxy.reportIssue(APPLICATION_DEPLOYMENT_HEALTH_REPORT, "Unable to identify the " +
                            "local host in the system model - please check your system-model.cfg.xml", Severity.BROKEN);
                }
            }
        }
    }

    private class SystemModelConfigListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        // TODO:Review - There's got to be a better way of initializing PowerFilter. Maybe the app management service could be queryable.
        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            currentSystemModel = configurationObject;

            // This event must be fired only after we have finished configuring the system.
            // This prevents a race condition illustrated below where the application
            // deployment event is caught but does nothing due to a null configuration
            synchronized (internalLock) {
                if (firstInitialization) {
                    firstInitialization = false;

                    eventService.newEvent(PowerFilterEvent.POWER_FILTER_CONFIGURED, System.currentTimeMillis());
                } else {
                    SystemModelInterrogator interrogator = new SystemModelInterrogator(clusterId, nodeId);

                    Optional<Node> ln = interrogator.getLocalNode(currentSystemModel);
                    Optional<ReposeCluster> lc = interrogator.getLocalCluster(currentSystemModel);
                    Optional<Destination> dd = interrogator.getDefaultDestination(currentSystemModel);

                    if (ln.isPresent() && lc.isPresent() && dd.isPresent()) {
                        serviceDomain = lc.get();
                        defaultDst = dd.get();

                        healthCheckServiceProxy.resolveIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT);

                        final List<FilterContext> newFilterChain = filterContextFactory.buildFilterContexts(getFilterConfig(), lc.get().getFilters().getFilter());
                        updateFilterChainBuilder(newFilterChain);
                    } else {
                        LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
                        healthCheckServiceProxy.reportIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT, "Unable to identify the " +
                                "local host in the system model - please check your system-model.cfg.xml", Severity.BROKEN);
                    }

                }
            }
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    private void updateFilterChainBuilder(List<FilterContext> newFilterChain) {
        synchronized (internalLock) {
            try {
                String dftDst = "";

                if (defaultDst != null) {
                    dftDst = defaultDst.getId();
                }
                SystemModelInterrogator smi = new SystemModelInterrogator(clusterId, nodeId);
                Node localHost = smi.getLocalNode(currentSystemModel).get();

                powerFilterChainBuilder.initialize(serviceDomain, localHost, newFilterChain, getFilterConfig().getServletContext(), dftDst);
            } catch (PowerFilterChainException ex) {
                LOG.error("Unable to initialize filter chain builder", ex);
            }
        }
        LOG.info("Repose ready");
    }

    @Override
    public void initFilterBean() {
        //TODO: maybe create power filter chain builder thingy
    }

    @Override
    public void destroy() {
        synchronized (internalLock) {
            if (powerFilterChainBuilder != null) {
                powerFilterChainBuilder.destroy();
            }
        }
    }

    private PowerFilterChain getRequestFilterChain(MutableHttpServletResponse mutableHttpResponse, FilterChain chain) throws ServletException {
        PowerFilterChain requestFilterChain = null;
        try {
            synchronized (internalLock) {
                //TODO: this should be done differently
                if (powerFilterChainBuilder == null) {
                    mutableHttpResponse.sendError(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue(), "Filter chain has not been initialized");
                } else if (!healthCheckService.isHealthy()) {
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
