/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.powerfilter;

import com.codahale.metrics.MetricRegistry;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.*;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.io.BufferedServletInputStream;
import org.openrepose.commons.utils.io.stream.LimitedReadInputStream;
import org.openrepose.commons.utils.logging.TracingHeaderHelper;
import org.openrepose.commons.utils.logging.TracingKey;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.servlet.http.ResponseMode;
import org.openrepose.core.filter.SystemModelInterrogator;
import org.openrepose.core.proxy.ServletContextWrapper;
import org.openrepose.core.services.RequestProxyService;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.deploy.ApplicationDeploymentEvent;
import org.openrepose.core.services.deploy.ArtifactManager;
import org.openrepose.core.services.event.Event;
import org.openrepose.core.services.event.EventListener;
import org.openrepose.core.services.event.EventService;
import org.openrepose.core.services.event.PowerFilterEvent;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;
import org.openrepose.core.services.jmx.ConfigurationInformation;
import org.openrepose.core.services.reporting.ReportingService;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.rms.ResponseMessageService;
import org.openrepose.core.services.uriredaction.UriRedactionService;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.config.*;
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService;
import org.openrepose.nodeservice.response.ResponseHeaderService;
import org.openrepose.powerfilter.filtercontext.FilterContext;
import org.openrepose.powerfilter.filtercontext.FilterContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrepose.commons.utils.http.CommonHttpHeader.*;
import static org.openrepose.commons.utils.opentracing.ScopeHelper.closeSpan;
import static org.openrepose.commons.utils.opentracing.ScopeHelper.startSpan;

/**
 * This class implements the Filter API and is managed by the servlet container.  This filter then loads
 * and runs the FilterChain which contains the individual filter instances listed in the system-model.cfg.xml.
 * <p/>
 * This class current instruments the response codes coming from Repose.
 * <p/>
 * THen the application context must be handed down into things that need it to build filters.
 * TODO: this also needs to check the properties to make sure they exist before we start up like the empty servlet used
 * to do.
 */
@Named("powerFilter")
public class PowerFilter extends DelegatingFilterProxy {
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";
    public static final String APPLICATION_DEPLOYMENT_HEALTH_REPORT = "ApplicationDeploymentError";
    private static final Logger LOG = LoggerFactory.getLogger(PowerFilter.class);
    private static final Logger TRACE_ID_LOG = LoggerFactory.getLogger(LOG.getName() + ".trace-id-logging");
    private static final List<String> SUPPORTED_HTTP_METHODS = Stream.of(
        HttpGet.METHOD_NAME,
        HttpPut.METHOD_NAME,
        HttpPost.METHOD_NAME,
        HttpDelete.METHOD_NAME,
        HttpHead.METHOD_NAME,
        HttpOptions.METHOD_NAME,
        HttpPatch.METHOD_NAME,
        HttpTrace.METHOD_NAME).collect(Collectors.toList());
    private final Object configurationLock = new Object();
    private final EventListener<ApplicationDeploymentEvent, List<String>> applicationDeploymentListener;
    private final UpdateListener<SystemModel> systemModelConfigurationListener;
    private final HealthCheckService healthCheckService;
    private final ContainerConfigurationService containerConfigurationService;
    private final ResponseMessageService responseMessageService;
    private final EventService eventService;
    private final FilterContextFactory filterContextFactory;

    private final AtomicReference<SystemModel> currentSystemModel = new AtomicReference<>();
    private final AtomicReference<PowerFilterRouter> powerFilterRouter = new AtomicReference<>();
    private final AtomicReference<List<FilterContext>> currentFilterChain = new AtomicReference<>();
    private final String nodeId;
    private final String clusterId;
    private final String reposeVersion;
    private final Tracer tracer;
    private final UriRedactionService uriRedactionService;
    private final PowerFilterRouterFactory powerFilterRouterFactory;
    private final ConfigurationService configurationService;
    private final Optional<MetricsService> metricsService;
    private final ConfigurationInformation configurationInformation;
    private final RequestProxyService requestProxyService;
    private final ArtifactManager artifactManager;
    private ReportingService reportingService;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private ResponseHeaderService responseHeaderService;

    /**
     * OMG SO MANY INJECTED THINGIES
     * TODO: make this less complex
     * TODO: For real, this is terrible
     *
     * @param clusterId                     this PowerFilter's cluster ID
     * @param nodeId                        this PowerFilter's node ID
     * @param reposeVersion                 The running version of repose
     * @param tracer                        the OpenTracing Tracer
     * @param powerFilterRouterFactory      Builds a powerfilter router for this power filter
     * @param reportingService              the reporting service
     * @param healthCheckService            the health check service
     * @param responseHeaderService         the response header service
     * @param configurationService          For monitoring config files
     * @param eventService                  the event service
     * @param containerConfigurationService the container configuration service
     * @param responseMessageService        the response message service
     * @param filterContextFactory          A factory that builds filter contexts
     * @param configurationInformation      allows JMX to see when this powerfilter is ready
     * @param requestProxyService           Only needed by the servletconfigwrapper thingy, no other way to get it in there
     * @param artifactManager               Needed to poll artifact loading to prevent prematurely constructing a PowerFilterChain
     * @param metricsService                the metrics service,
     * @param uriRedactionService           the URI Redaction service
     */
    @Inject
    public PowerFilter(
        @Value(ReposeSpringProperties.NODE.CLUSTER_ID) String clusterId,
        @Value(ReposeSpringProperties.NODE.NODE_ID) String nodeId,
        @Value(ReposeSpringProperties.CORE.REPOSE_VERSION) String reposeVersion,
        Tracer tracer,
        PowerFilterRouterFactory powerFilterRouterFactory,
        ReportingService reportingService,
        HealthCheckService healthCheckService,
        ResponseHeaderService responseHeaderService,
        ConfigurationService configurationService,
        EventService eventService,
        ContainerConfigurationService containerConfigurationService,
        ResponseMessageService responseMessageService,
        FilterContextFactory filterContextFactory,
        ConfigurationInformation configurationInformation,
        RequestProxyService requestProxyService,
        ArtifactManager artifactManager,
        Optional<MetricsService> metricsService,
        UriRedactionService uriRedactionService
    ) {
        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.reposeVersion = reposeVersion;
        this.powerFilterRouterFactory = powerFilterRouterFactory;
        this.configurationService = configurationService;
        this.metricsService = metricsService;
        this.tracer = tracer;
        this.configurationInformation = configurationInformation;
        this.requestProxyService = requestProxyService;
        this.artifactManager = artifactManager;
        this.uriRedactionService = uriRedactionService;

        // Set up the configuration listeners
        systemModelConfigurationListener = new SystemModelConfigListener();
        applicationDeploymentListener = new ApplicationDeploymentEventListener();

        this.responseHeaderService = responseHeaderService;
        this.reportingService = reportingService;
        this.containerConfigurationService = containerConfigurationService;
        this.responseMessageService = responseMessageService;
        this.eventService = eventService;
        this.filterContextFactory = filterContextFactory;

        this.healthCheckService = healthCheckService;

        healthCheckServiceProxy = healthCheckService.register();
    }

    public static void markResponseCodeHelper(MetricsService metricsService, int responseCode, Logger log, String component) {
        int code = responseCode / 100;
        String meterId = null;
        if (1 < code && code < 6) {
            meterId = String.format("%dXX", code);
        }
        if (meterId != null) {
            metricsService.getRegistry()
                .meter(MetricRegistry.name("org.openrepose.core.ResponseCode", component, meterId))
                .mark();
        } else {
            log.error((component != null ? component + ":  " : "") + "Encountered invalid response code: " + responseCode);
        }
    }

    /**
     * Triggered each time the event service triggers an app deploy and when the system model is updated.
     */
    private void configurationHeartbeat() {
        if (currentSystemModel.get() != null && artifactManager.allArtifactsLoaded()) {
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
                        //Sometimes we won't have any filters
                        FilterList listOfFilters = localCluster.get().getFilters();

                        //Only if we've been configured with some filters should we get a new list
                        List<FilterContext> newFilterChain;
                        if (listOfFilters != null) {
                            //TODO: sometimes there isn't any FilterConfig available, and it'll be null...
                            newFilterChain = filterContextFactory.buildFilterContexts(getServletContext(), listOfFilters.getFilter());
                        } else {
                            //Running with no filters is a totally valid use case!
                            newFilterChain = Collections.emptyList();
                        }

                        List<FilterContext> oldFilterChain = currentFilterChain.getAndSet(newFilterChain);

                        powerFilterRouter.set(powerFilterRouterFactory.
                            getPowerFilterRouter(serviceDomain, localNode.get(), getServletContext(), defaultDst.getId()));

                        //Destroy all the old filters
                        if (oldFilterChain != null) {
                            for (FilterContext ctx : oldFilterChain) {
                                ctx.destroy();
                            }
                        }

                        if (LOG.isDebugEnabled()) {
                            List<String> filterChainInfo = new LinkedList<>();
                            for (FilterContext ctx : newFilterChain) {
                                filterChainInfo.add(ctx.getName() + "-" + ctx.getFilter().getClass().getName());
                            }
                            LOG.debug("{}:{} -- Repose filter chain: {}", clusterId, nodeId, filterChainInfo);
                        }

                        //Only log this repose ready if we're able to properly fire up a new filter chain
                        LOG.info("{}:{} -- Repose ready", clusterId, nodeId);
                        //Update the JMX bean with our status
                        configurationInformation.updateNodeStatus(clusterId, nodeId, true);
                    } catch (FilterInitializationException fie) {
                        LOG.error("{}:{} -- Unable to create new filter chain.", clusterId, nodeId, fie);
                        //Update the JMX bean with our status
                        configurationInformation.updateNodeStatus(clusterId, nodeId, false);
                    } catch (PowerFilterChainException e) {
                        LOG.error("{}:{} -- Unable to initialize filter chain builder.", clusterId, nodeId, e);
                        //Update the JMX bean with our status
                        configurationInformation.updateNodeStatus(clusterId, nodeId, false);
                    }
                } else {
                    LOG.error("{}:{} -- Unhealthy system-model config (cannot identify local node, or no default destination) - please check your system-model.cfg.xml", clusterId, nodeId);
                    healthCheckServiceProxy.reportIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT, "Unable to identify the " +
                        "local host in the system model, or no default destination - please check your system-model.cfg.xml", Severity.BROKEN);
                }
            }
        }
    }

    @Override
    public void initFilterBean() {
        LOG.info("{}:{} -- Initializing PowerFilter bean", clusterId, nodeId);

        /*
         * http://docs.spring.io/spring-framework/docs/3.1.4.RELEASE/javadoc-api/org/springframework/web/filter/GenericFilterBean.html#setServletContext%28javax.servlet.ServletContext%29
         * Configure the servlet Context wrapper insanity to get to the Request Dispatcher I think...
         * NOTE: this thing alone provides the dispatcher for forwarding requests. It's really kind of gross.
         * we should seriously consider doing it in a ProxyServlet or something. Far less complicated.
         * getFilterConfig might be null sometimes, so just wrap it with existing servlet context
         *
         * TODO: this is broke if we set the container to create the FilterConfig, of course that doesn't give us a filterConfig either...
         */
        ServletContextWrapper wrappedServletContext = new ServletContextWrapper(getServletContext(), requestProxyService);
        setServletContext(wrappedServletContext);

        eventService.listen(applicationDeploymentListener, ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED);

        URL xsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        configurationService.subscribeTo("", "system-model.cfg.xml", xsdURL, systemModelConfigurationListener, SystemModel.class);
    }

    @Override
    public void destroy() {
        healthCheckServiceProxy.deregister();
        LOG.info("{}:{} -- Destroying PowerFilter bean", clusterId, nodeId);
        eventService.squelch(applicationDeploymentListener, ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED);
        configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelConfigurationListener);

        //TODO: do we need to synchronize on the configuration lock?
        if (currentFilterChain.get() != null) {
            for (FilterContext context : currentFilterChain.get()) {
                context.destroy();
            }
        }
    }

    private PowerFilterChain getRequestFilterChain(HttpServletResponse httpResponse, FilterChain chain) throws ServletException, IOException {
        PowerFilterChain requestFilterChain = null;
        try {
            boolean healthy = healthCheckService.isHealthy();
            List<FilterContext> filterChain = currentFilterChain.get();
            PowerFilterRouter router = powerFilterRouter.get();

            if (!healthy ||
                filterChain == null ||
                router == null) {
                LOG.warn("{}:{} -- Repose is not ready!", clusterId, nodeId);
                LOG.debug("{}:{} -- Health status: {}", clusterId, nodeId, healthy);
                LOG.debug("{}:{} -- Current filter chain: {}", clusterId, nodeId, filterChain);
                LOG.debug("{}:{} -- Power Filter Router: {}", clusterId, nodeId, router);

                httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Currently unable to serve requests");

                //Update the JMX bean with our status
                configurationInformation.updateNodeStatus(clusterId, nodeId, false);
            } else {
                requestFilterChain = new PowerFilterChain(filterChain, chain, router, metricsService,
                    Optional.ofNullable(currentSystemModel.get().getReposeCluster().stream()
                        .filter(cluster -> cluster.getId().equals(clusterId)).findFirst()
                        .get().getFilters()).map(FilterList::getBypassUriRegex));
            }
        } catch (PowerFilterChainException ex) {
            LOG.warn("{}:{} -- Error creating filter chain", clusterId, nodeId, ex);
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Error creating filter chain");

            //Update the JMX bean with our status
            configurationInformation.updateNodeStatus(clusterId, nodeId, false);
        }

        return requestFilterChain;
    }

    @Override
    @SuppressWarnings("squid:S1848")
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final long startTime = System.currentTimeMillis();

        Optional.ofNullable(((HttpServletRequest) request).getHeader(PowerApiHeader.TRACE_REQUEST))
            .ifPresent(__ -> MDC.put(PowerApiHeader.TRACE_REQUEST, "true"));

        final Optional<Long> contentBodyReadLimit = containerConfigurationService.getContentBodyReadLimit();
        final InputStream requestBodyInputStream = contentBodyReadLimit.isPresent() ?
            new LimitedReadInputStream(contentBodyReadLimit.get(), request.getInputStream()) :
            request.getInputStream();

        // todo: Use the Java 8 functional interfaces once they support rethrowing exceptions
        // final InputStream requestBodyInputStream = containerConfigurationService.getContentBodyReadLimit()
        //        .map(l -> new LimitedReadInputStream(l, request.getInputStream()))
        //        .orElseGet(request.getInputStream());

        final HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper((HttpServletResponse) response,
            ResponseMode.MUTABLE,
            ResponseMode.MUTABLE);
        final BufferedServletInputStream bufferedInputStream = new BufferedServletInputStream(requestBodyInputStream);
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper((HttpServletRequest) request,
            bufferedInputStream);

        // Since getParameterMap may read the body, we must reset the InputStream so that we aren't stripping
        // the body when form parameters are sent.
        bufferedInputStream.mark(Integer.MAX_VALUE);
        wrappedRequest.setAttribute("http://openrepose.org/queryParams", wrappedRequest.getParameterMap());
        bufferedInputStream.reset();

        // Re-wrapping the request to reset the inputStream/Reader flag
        wrappedRequest = new HttpServletRequestWrapper((HttpServletRequest) request, bufferedInputStream);

        Scope scope = startSpan(wrappedRequest, tracer, LOG, Tags.SPAN_KIND_CLIENT, reposeVersion, uriRedactionService);

        if (currentSystemModel.get().getTracingHeader() != null && currentSystemModel.get().getTracingHeader().isRewriteHeader()) {
            wrappedRequest.removeHeader(TRACE_GUID);
        }

        //Grab the traceGUID from the request if there is one, else create one
        String traceGUID;
        if (StringUtils.isBlank(wrappedRequest.getHeader(TRACE_GUID))) {
            traceGUID = UUID.randomUUID().toString();
        } else {
            traceGUID = TracingHeaderHelper.getTraceGuid(wrappedRequest.getHeader(TRACE_GUID));
        }

        MDC.put(TracingKey.TRACING_KEY, traceGUID);

        try {
            // Ensures that the method name is supported
            // todo: HTTP request methods are case-sensitive, so this check should not upper case the request method
            if (!SUPPORTED_HTTP_METHODS.contains(wrappedRequest.getMethod().toUpperCase())) {
                throw new InvalidMethodException(wrappedRequest.getMethod() + " method not supported");
            }
            // Ensure the request URI is a valid URI
            // This object is only being created to ensure its validity.
            // So it is safe to suppress warning squid:S1848
            new URI(wrappedRequest.getRequestURI());
            final PowerFilterChain requestFilterChain = getRequestFilterChain(wrappedResponse, chain);
            if (requestFilterChain != null) {
                if (currentSystemModel.get().getTracingHeader() == null ||
                    currentSystemModel.get().getTracingHeader().isEnabled()) {
                    if (StringUtils.isBlank(wrappedRequest.getHeader(TRACE_GUID))) {
                        wrappedRequest.addHeader(TRACE_GUID,
                            TracingHeaderHelper.createTracingHeader(traceGUID, wrappedRequest.getHeader(VIA)));
                    }
                    if ((currentSystemModel.get().getTracingHeader() != null) &&
                        currentSystemModel.get().getTracingHeader().isSecondaryPlainText()) {
                        TRACE_ID_LOG.trace("Adding plain text trans id to request: {}", traceGUID);
                        wrappedRequest.replaceHeader(REQUEST_ID, traceGUID);
                    }
                    String tracingHeader = wrappedRequest.getHeader(TRACE_GUID);
                    TRACE_ID_LOG.info("Tracing header: {}", TracingHeaderHelper.decode(tracingHeader));
                    wrappedResponse.addHeader(TRACE_GUID, tracingHeader);
                }

                requestFilterChain.startFilterChain(wrappedRequest, wrappedResponse);
            }
        } catch (InvalidMethodException ime) {
            LOG.debug("{}:{} -- Invalid HTTP method requested: {}", clusterId, nodeId, wrappedRequest.getMethod(), ime);
            wrappedResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error processing request");
        } catch (URISyntaxException use) {
            LOG.debug("{}:{} -- Invalid URI requested: {}", clusterId, nodeId, wrappedRequest.getRequestURI(), use);
            wrappedResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error processing request");
        } catch (Exception ex) {
            LOG.error("{}:{} -- Issue encountered while processing filter chain.", clusterId, nodeId, ex);
            wrappedResponse.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Error processing request");
        } catch (Error e) {
            LOG.error("{}:{} -- Error encountered while processing filter chain.", clusterId, nodeId, e);
            wrappedResponse.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Error processing request");
            throw e;
        } finally {
            // We must call this method here so that the response messaging service can potentially mutate the body of
            // the response. The method itself enables the wrapper to report a committed response as being so, while
            // still allowing the component which wrapped the response to mutate the response.
            wrappedResponse.uncommit();

            closeSpan(wrappedResponse, scope);

            // In the case where we pass/route the request, there is a chance that
            // the response will be committed by an underlying service, outside of repose
            if (!wrappedResponse.isCommitted()) {
                responseHeaderService.setVia(wrappedRequest, wrappedResponse);
                responseMessageService.handle(wrappedRequest, wrappedResponse);
            }

            wrappedResponse.commitToResponse();

            final long stopTime = System.currentTimeMillis();

            metricsService.ifPresent(ms -> markResponseCodeHelper(ms, ((HttpServletResponse) response).getStatus(), LOG, "Repose"));

            reportingService.incrementReposeStatusCodeCount(((HttpServletResponse) response).getStatus(), stopTime - startTime);
        }
        // Clear out the logger context now that we are done with this request
        MDC.clear();
    }

    private class ApplicationDeploymentEventListener implements EventListener<ApplicationDeploymentEvent, List<String>> {

        @Override
        public void onEvent(Event<ApplicationDeploymentEvent, List<String>> e) {
            LOG.info("{}:{} -- Application collection has been modified. Application that changed: {}", clusterId, nodeId, e.payload());

            // Using a set instead of a list to have a deployment health report if there are multiple artifacts with the same name
            Set<String> uniqueArtifacts = new HashSet<>();
            try {
                for (String artifactName : e.payload()) {
                    uniqueArtifacts.add(artifactName);
                }
                healthCheckServiceProxy.resolveIssue(APPLICATION_DEPLOYMENT_HEALTH_REPORT);
            } catch (IllegalArgumentException exception) {
                healthCheckServiceProxy.reportIssue(APPLICATION_DEPLOYMENT_HEALTH_REPORT,
                    "Please review your artifacts directory, multiple versions of the same artifact exist!",
                    Severity.BROKEN);
                LOG.error("Please review your artifacts directory, multiple versions of same artifact exists.");
                LOG.trace("", exception);
            }

            configurationHeartbeat();
        }
    }

    private class SystemModelConfigListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            //TODO: how did I get here, when I've unsubscribed!
            LOG.debug("{}:{} New system model configuration provided", clusterId, nodeId);
            SystemModel previousSystemModel = currentSystemModel.getAndSet(configurationObject);
            //TODO: is this wrong?
            if (previousSystemModel == null) {
                LOG.debug("{}:{} -- issuing POWER_FILTER_CONFIGURED event from a configuration update", clusterId, nodeId);
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
}
