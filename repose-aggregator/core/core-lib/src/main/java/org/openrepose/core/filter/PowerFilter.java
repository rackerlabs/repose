package org.openrepose.core.filter;

import com.google.common.base.Optional;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.servlet.filter.ApplicationContextAwareFilter;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.ResponseCode;
import org.openrepose.core.domain.ServicePorts;
import org.openrepose.core.services.context.ContextAdapter;
import org.openrepose.core.services.context.ServletContextHelper;
import org.openrepose.core.services.deploy.ApplicationDeploymentEvent;
import org.openrepose.core.services.event.PowerFilterEvent;
import org.openrepose.core.services.event.common.Event;
import org.openrepose.core.services.event.common.EventListener;
import org.openrepose.core.services.headers.response.ResponseHeaderService;
import org.openrepose.core.services.reporting.ReportingService;
import org.openrepose.core.services.reporting.metrics.MeterByCategory;
import org.openrepose.core.systemmodel.Destination;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
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
 */
public class PowerFilter extends ApplicationContextAwareFilter {
    private static final Logger LOG = LoggerFactory.getLogger(PowerFilter.class);
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";
    public static final String APPLICATION_DEPLOYMENT_HEALTH_REPORT = "ApplicationDeploymentError";

    private final Object internalLock = new Object();
    private final EventListener<ApplicationDeploymentEvent, List<String>> applicationDeploymentListener;
    private final UpdateListener<SystemModel> systemModelConfigurationListener;

    private boolean firstInitialization;
    private ServicePorts ports;
    private PowerFilterChainBuilder powerFilterChainBuilder;
    private ContextAdapter papiContext;
    private SystemModel currentSystemModel;
    private ReposeCluster serviceDomain;
    private Node localHost;
    private FilterConfig filterConfig;
    private ReportingService reportingService;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private MeterByCategory mbcResponseCodes;
    private ResponseHeaderService responseHeaderService;
    private Destination defaultDst;

    public PowerFilter() {
        firstInitialization = true;

        // Default to an empty filter chain so that artifact deployment doesn't gum up the works with a null pointer
        powerFilterChainBuilder = null;
        systemModelConfigurationListener = new SystemModelConfigListener();
        applicationDeploymentListener = new ApplicationDeploymentEventListener();
    }

    private class ApplicationDeploymentEventListener implements EventListener<ApplicationDeploymentEvent, List<String>> {

        @Override
        public void onEvent(Event<ApplicationDeploymentEvent, List<String>> e) {
            LOG.info("Application collection has been modified. Application that changed: " + e.payload());

            List<String> uniqueArtifactList = new ArrayList<>();

            for (String artifactName : e.payload()) {
                if (!uniqueArtifactList.contains(artifactName)) {
                    uniqueArtifactList.add(artifactName);
                } else {
                    LOG.error("Please review your artifacts directory, multiple versions of same artifact exists.");
                }
            }

            if (currentSystemModel != null) {
                SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);

                Optional<Node> ln = interrogator.getLocalNode(currentSystemModel);
                Optional<ReposeCluster> lc = interrogator.getLocalCluster(currentSystemModel);
                Optional<Destination> dd = interrogator.getDefaultDestination(currentSystemModel);

                if (ln.isPresent() && lc.isPresent() && dd.isPresent()) {
                    localHost = ln.get();
                    serviceDomain = lc.get();
                    defaultDst = dd.get();
                    healthCheckServiceProxy.resolveIssue(APPLICATION_DEPLOYMENT_HEALTH_REPORT);
                } else {
                    // Note: This should never occur! If it does, the currentSystemModel is being set to something
                    // invalid, and that should be prevented in the SystemModelConfigListener below. Resolution of
                    // this issue will only occur when the config is fixed and the application is redeployed.
                    LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
                    healthCheckServiceProxy.reportIssue(APPLICATION_DEPLOYMENT_HEALTH_REPORT, "Unable to identify the " +
                            "local host in the system model - please check your system-model.cfg.xml", Severity.BROKEN);
                }

                final List<FilterContext> newFilterChain = new FilterContextInitializer(
                        filterConfig,
                        ServletContextHelper.getInstance(filterConfig.getServletContext()).getApplicationContext()).buildFilterContexts(papiContext.classLoader(), serviceDomain, localHost);

                updateFilterChainBuilder(newFilterChain);
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

                    papiContext.eventService().newEvent(PowerFilterEvent.POWER_FILTER_CONFIGURED, System.currentTimeMillis());
                } else {
                    SystemModelInterrogator interrogator = new SystemModelInterrogator(ports);

                    Optional<Node> ln = interrogator.getLocalNode(currentSystemModel);
                    Optional<ReposeCluster> lc = interrogator.getLocalCluster(currentSystemModel);
                    Optional<Destination> dd = interrogator.getDefaultDestination(currentSystemModel);

                    if (ln.isPresent() && lc.isPresent() && dd.isPresent()) {
                        localHost = ln.get();
                        serviceDomain = lc.get();
                        defaultDst = dd.get();

                        healthCheckServiceProxy.resolveIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT);
                    } else {
                        LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
                        healthCheckServiceProxy.reportIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT, "Unable to identify the " +
                                "local host in the system model - please check your system-model.cfg.xml", Severity.BROKEN);
                    }

                    final List<FilterContext> newFilterChain = new FilterContextInitializer(
                            filterConfig,
                            ServletContextHelper.getInstance(filterConfig.getServletContext()).getApplicationContext()).buildFilterContexts(papiContext.classLoader(), serviceDomain, localHost);
                    updateFilterChainBuilder(newFilterChain);
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
                powerFilterChainBuilder = papiContext.filterChainBuilder();
                powerFilterChainBuilder.initialize(serviceDomain, localHost, newFilterChain, filterConfig.getServletContext(), dftDst);
            } catch (PowerFilterChainException ex) {
                LOG.error("Unable to initialize filter chain builder", ex);
            }
        }
        LOG.info("Repose ready");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        this.filterConfig = filterConfig;
        ServletContextHelper servletContextHelper = ServletContextHelper.getInstance(filterConfig.getServletContext());

        ports = servletContextHelper.getServerPorts();
        papiContext = servletContextHelper.getPowerApiContext();

        papiContext.eventService().listen(applicationDeploymentListener, ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED);
        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        papiContext.configurationService().subscribeTo("", "system-model.cfg.xml", xsdURL, systemModelConfigurationListener, SystemModel.class);

        filterConfig.getServletContext().setAttribute("powerFilter", this);

        reportingService = papiContext.reportingService();
        responseHeaderService = papiContext.responseHeaderService();

        healthCheckServiceProxy = papiContext.healthCheckService().register();

        if (papiContext.metricsService() != null) {
            mbcResponseCodes = papiContext.metricsService().newMeterByCategory(ResponseCode.class, "Repose", "Response Code", TimeUnit.SECONDS);
        }
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
                if (powerFilterChainBuilder == null) {
                    mutableHttpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Filter chain has not been initialized");
                } else if (!papiContext.healthCheckService().isHealthy()) {
                    mutableHttpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Currently unable to serve requests");
                } else {
                    requestFilterChain = powerFilterChainBuilder.newPowerFilterChain(chain);
                }
            }
        } catch (PowerFilterChainException ex) {
            LOG.warn("Error creating filter chain", ex);
            mutableHttpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE , "Error creating filter chain");
            mutableHttpResponse.setLastException(ex);
        }

        return requestFilterChain;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final long startTime = System.currentTimeMillis();
        long streamLimit = papiContext.containerConfigurationService().getContentBodyReadLimit();
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
            mutableHttpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error processing request");
            mutableHttpResponse.setLastException(use);
        } catch (Exception ex) {
            LOG.error("Exception encountered while processing filter chain. Reason: " + ex.getMessage(), ex);
            mutableHttpResponse.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Error processing request");
            mutableHttpResponse.setLastException(ex);
        } finally {
            // In the case where we pass/route the request, there is a chance that
            // the response will be committed by an underlying service, outside of repose
            if (!mutableHttpResponse.isCommitted()) {
                papiContext.responseMessageService().handle(mutableHttpRequest, mutableHttpResponse);
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
