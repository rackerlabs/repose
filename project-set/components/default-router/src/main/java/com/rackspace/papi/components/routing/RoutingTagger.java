package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

public class RoutingTagger extends AbstractFilterLogicHandler {

   private static final Logger LOG = LoggerFactory.getLogger(RoutingTagger.class);
   private final SystemModelInterrogator modelInterrogator;
    private SystemModel model;

   public RoutingTagger(SystemModelInterrogator modelInterrogator) {
      this.modelInterrogator = modelInterrogator;
   }
   
   public RoutingTagger setSystemModel(SystemModel model) {
       this.model = model;
       return this;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.PASS);

      Destination defaultDest = modelInterrogator.getDefaultDestination(model);
      
      if (defaultDest != null) {
         myDirector.addDestination(defaultDest, request.getRequestURI(), -1);
      } else {
         LOG.warn("No default destination configured for service domain: " + modelInterrogator.getLocalServiceDomain(model).getId());
      }

      return myDirector;
   }

   @Override
   public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
      return super.handleResponse(request, response);
   }
}
