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
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.openrepose.components.authz.rackspace.config.ServiceEndpoint;
import org.openrepose.components.rackspace.authz.cache.CachedEndpoint;
import org.openrepose.components.rackspace.authz.cache.EndpointListCache;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestAuthroizationHandler extends AbstractFilterLogicHandler {

   private static final Logger LOG = LoggerFactory.getLogger(RequestAuthroizationHandler.class);
   private final OpenStackAuthenticationService authenticationService;
   private final EndpointListCache endpointListCache;
   private final ServiceEndpoint myEndpoint;

   public RequestAuthroizationHandler(OpenStackAuthenticationService authenticationService, EndpointListCache endpointListCache, ServiceEndpoint myEndpoint) {
      this.authenticationService = authenticationService;
      this.endpointListCache = endpointListCache;
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
      final List<CachedEndpoint> authorizedEndpoints = getEndpointsForToken(userToken);

      for (CachedEndpoint authorizedEndpoint : authorizedEndpoints) {
         if (authorizedEndpoint.getPublicUrl().startsWith(myEndpoint.getHref())) {
            director.setFilterAction(FilterAction.PASS);
            break;
         }
      }
   }

   private List<CachedEndpoint> getEndpointsForToken(String userToken) {
      List<CachedEndpoint> cachedEnpoints = endpointListCache.getCachedEndpointsForToken(userToken);

      if (cachedEnpoints == null) {
         cachedEnpoints = requestEnpointsForToeknFromAuthService(userToken);

         try {
            endpointListCache.cacheEndpointsForToken(userToken, cachedEnpoints);
         } catch (IOException ioe) {
            LOG.error("Caching failure. Reason: " + ioe.getMessage(), ioe);
         }
      }

      return cachedEnpoints;
   }

   private List<CachedEndpoint> requestEnpointsForToeknFromAuthService(String userToken) {
      final List<Endpoint> authorizedEndpoints = authenticationService.getEndpointsForToken(userToken);
      final LinkedList<CachedEndpoint> serializable = new LinkedList<CachedEndpoint>();

      for (Endpoint ep : authorizedEndpoints) {
         serializable.add(new CachedEndpoint(ep.getPublicURL()));
      }

      return serializable;
   }

   public boolean authenticationWasDelegated(HttpServletRequest request) {
      final String wwwAuthenticateHeader = request.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString());

      return !StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains("Delegated");
   }
}
