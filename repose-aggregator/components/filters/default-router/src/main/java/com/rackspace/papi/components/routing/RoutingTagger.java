package com.rackspace.papi.components.routing;

import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
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
