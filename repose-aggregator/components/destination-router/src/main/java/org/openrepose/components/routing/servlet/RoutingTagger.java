package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.filters.DestinationRouter;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.service.reporting.metrics.impl.MeterByCategorySum;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

public class RoutingTagger extends AbstractFilterLogicHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RoutingTagger.class);
    private String id;
    private double quality;
    private MetricsService metricsService;
    private MeterByCategorySum mbcsRoutedReponse;

    public RoutingTagger(String id, double quality, MetricsService metricsService) {
        this.quality = quality;
        this.id = id;
        this.metricsService = metricsService;

        if (metricsService != null) {
            mbcsRoutedReponse = metricsService.newMeterByCategorySum(DestinationRouter.class, "destination-router",
                    "Routed Response", TimeUnit.SECONDS);
        }
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);

        if (StringUtilities.isBlank(id)) {
            LOG.warn("No Destination configured for Destination Router");
        } else {
            myDirector.addDestination(id, request.getRequestURI(), quality);
            if (mbcsRoutedReponse != null) {
                mbcsRoutedReponse.mark(id);
            }
        }

        return myDirector;
    }
}
