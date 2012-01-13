package org.openrepose.components.rackspace.authz;

import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;

public class RequestAuthroizationHandler extends AbstractFilterLogicHandler {

   private final OpenStackAuthenticationService authenticationService;

   public RequestAuthroizationHandler(OpenStackAuthenticationService authenticationService) {
      this.authenticationService = authenticationService;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.PASS);
      
      return myDirector;
   }
}
