package org.openrepose.filters.defaultrouter.routing;

import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.model.Destination;


import javax.servlet.http.HttpServletRequest;

public class RoutingTagger extends AbstractFilterLogicHandler {

   private Destination defaultDest;

   public RoutingTagger() {
   }
   
   
   public RoutingTagger setDestination(Destination dst){
       defaultDest = dst;
       return this;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.PASS);
      
      if (defaultDest != null) {
         myDirector.addDestination(defaultDest, request.getRequestURI(), -1);
      }
      return myDirector;
   }
}
