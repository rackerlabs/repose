package org.openrepose.components.rackspace.authz;

import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.openrepose.components.authz.rackspace.config.ServiceEndpoint;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestAuthroizationHandler extends AbstractFilterLogicHandler {

   private static final Logger LOG = LoggerFactory.getLogger(RequestAuthroizationHandler.class);
   private final OpenStackAuthenticationService authenticationService;
   private final ServiceEndpoint myEndpoint;

   public RequestAuthroizationHandler(OpenStackAuthenticationService authenticationService, ServiceEndpoint myEndpoint) {
      this.authenticationService = authenticationService;
      this.myEndpoint = myEndpoint;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.RETURN);
      myDirector.setResponseStatus(HttpStatusCode.FORBIDDEN);

      if (authenticationWasDelegated(request)) {
         // We do not support delegation
         LOG.debug("Authentication delegation is not supported by the rackspace authorization filter. Rejecting request.");
      } else {
         authorizeRequest(myDirector, request);
      }

      return myDirector;
   }

   public void authorizeRequest(FilterDirector director, HttpServletRequest request) {
      final String authenticationToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString());

      if (StringUtilities.isBlank(authenticationToken)) {
         // We do not support delegation
         LOG.debug("Authentication delegation is not supported unauthorized requests. Rejecting request.");
         director.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
      } else {
         checkTenantEndpoints(director, authenticationToken);
      }
   }

   public void checkTenantEndpoints(FilterDirector director, String userToken) {
      final List<Endpoint> authorizedEndpoints = authenticationService.getEndpointsForToken(userToken);

      for (Endpoint authorizedEndpoint : authorizedEndpoints) {
         if (authorizedEndpoint.getPublicURL().equals(myEndpoint.getHref())) {
            director.setFilterAction(FilterAction.PASS);
            break;
         }
      }
   }

   public boolean authenticationWasDelegated(HttpServletRequest request) {
      final String wwwAuthenticateHeader = request.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString());

      return !StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains("Delegated");
   }
}
