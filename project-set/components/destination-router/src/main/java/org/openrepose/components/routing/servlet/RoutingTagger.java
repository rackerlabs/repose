package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class RoutingTagger extends AbstractFilterLogicHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RoutingTagger.class);
    private static final String DEFAULT_QUALITY = "0.5";
    private String id;
    private float quality;

    public RoutingTagger(String id, float quality) {
        this.quality = quality;
        this.id = id;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);

        if (StringUtilities.isBlank(id)) {
            LOG.warn("No Destination configured for Destination Router");
        } else {
            myDirector.addDestination(id, request.getRequestURI(), quality);
        }

        return myDirector;
    }
}
