package org.openrepose.powerfilter;

import org.openrepose.core.filter.routing.DestinationLocationBuilder;
import org.openrepose.core.services.headers.response.ResponseHeaderService;
import org.openrepose.core.services.reporting.ReportingService;
import org.openrepose.core.services.reporting.metrics.MeterByCategory;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.routing.RoutingService;
import org.openrepose.core.systemmodel.Destination;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.nodeservice.request.RequestHeaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Named
public class PowerFilterRouterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterRouterFactory.class);
    private final ReportingService reportingService;
    private final RequestHeaderService requestHeaderService;
    private final ResponseHeaderService responseHeaderService;
    private final RoutingService routingService;

    private MetricsService metricsService;

    //These are here and not in the impl, because we want them to stay around if the router changes
    private ConcurrentHashMap<String, MeterByCategory> mapResponseCodes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, MeterByCategory> mapRequestTimeouts = new ConcurrentHashMap<>();

    @Inject
    public PowerFilterRouterFactory(
            MetricsService metricsService,
            ReportingService reportingService,
            RequestHeaderService requestHeaderService,
            ResponseHeaderService responseHeaderService,
            RoutingService routingService) {
        LOG.info("Creating Repose Router Factory!");
        this.routingService = routingService;
        this.reportingService = reportingService;
        this.responseHeaderService = responseHeaderService;
        this.requestHeaderService = requestHeaderService;
        this.metricsService = metricsService;
    }

    public PowerFilterRouter getPowerFilterRouter(ReposeCluster domain,
                                                  Node localhost,
                                                  ServletContext servletContext,
                                                  String defaultDestination) throws PowerFilterChainException {
        LOG.info("Reticulating Splines - Power Filter Router");
        if (localhost == null || domain == null) {
            //TODO: THIS IS STOOPID
            throw new PowerFilterChainException("Domain and localhost cannot be null");
        }

        DestinationLocationBuilder locationBuilder = new DestinationLocationBuilder(routingService, localhost);
        Map<String, Destination> destinations = new HashMap<>();
        //Set up location builder
        if (domain.getDestinations() != null) {
            addDestinations(domain.getDestinations().getEndpoint(), destinations);
            addDestinations(domain.getDestinations().getTarget(), destinations);
        }

        return new PowerFilterRouterImpl(locationBuilder,
                destinations,
                domain,
                defaultDestination,
                servletContext,
                requestHeaderService,
                responseHeaderService,
                metricsService,
                mapResponseCodes,
                mapRequestTimeouts,
                reportingService);
    }

    private void addDestinations(List<? extends Destination> destList, Map<String, Destination> targetList) {
        for (Destination dest : destList) {
            targetList.put(dest.getId(), dest);
        }
    }


}
