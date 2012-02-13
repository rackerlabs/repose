package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.openrepose.components.routing.servlet.config.ContextPathRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingTagger extends AbstractFilterLogicHandler {

   private static final Logger LOG = LoggerFactory.getLogger(RoutingTagger.class);
   private final List<ContextPathRoute> routes;

   public RoutingTagger(List<ContextPathRoute> routes) {
      this.routes = routes;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.PASS);

      for (ContextPathRoute route : routes) {
         LOG.debug("Adding route: " + route.getValue());
         String localDestination = route.getValue() + request.getRequestURI();
         myDirector.requestHeaderManager().appendHeader(PowerApiHeader.NEXT_ROUTE.toString(), route.getValue());
         myDirector.setRequestUri(localDestination);
         //myDirector.setRequestUrl(new StringBuffer(localDestination));
      }
      
      return myDirector;
   }
}
