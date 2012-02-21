package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.QualityFactorUtility;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.openrepose.components.routing.servlet.config.ContextPathRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingTagger extends AbstractFilterLogicHandler {

   private static final Logger LOG = LoggerFactory.getLogger(RoutingTagger.class);
   private static final String NEXT_ROUTE_HEADER = PowerApiHeader.NEXT_ROUTE.toString();
   private final List<ContextPathRoute> routes;

   public RoutingTagger(List<ContextPathRoute> routes) {
      this.routes = routes;
   }
   
   private String determineRequestUri(Set<String> possibleRoutes) {
      // Remove this code once we have a dispatcher that can handle quality
      final List<HeaderValue> routes = new HeaderFieldParser(possibleRoutes).parse();
      return QualityFactorUtility.choosePreferredHeaderValue(routes).getValue();
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      final HeaderManager headerManager = myDirector.requestHeaderManager();
      myDirector.setFilterAction(FilterAction.PASS);

      for (ContextPathRoute route : routes) {
         String routeDestination = route.getValue() + request.getRequestURI();
         headerManager.appendHeader(NEXT_ROUTE_HEADER, routeDestination);
         LOG.debug("Adding route: " + routeDestination);
     }

      final String uri = determineRequestUri(headerManager.headersToAdd().get(NEXT_ROUTE_HEADER));
      myDirector.setRequestUri(uri);
      
      return myDirector;
   }
}
