package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.openrepose.components.routing.servlet.config.ContextPathRoute;
import org.slf4j.LoggerFactory;

public class RoutingTagger extends AbstractFilterLogicHandler {

   private final List<HeaderValue> headerValues;

   public RoutingTagger(List<ContextPathRoute> routes) {
      headerValues = new LinkedList<HeaderValue>();
      
      for (ContextPathRoute route : routes) {
         headerValues.add(new HeaderValueImpl(route.getValue(), route.getQualityFactor()));
      }
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.PASS);

      for (HeaderValue route : headerValues) {
         myDirector.requestHeaderManager().appendHeader(PowerApiHeader.NEXT_ROUTE.getHeaderKey(), route.toString());
      }
      
      return myDirector;
   }
}
