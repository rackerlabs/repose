package org.openrepose.components.rackspace.authz;

import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
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

public class RequestAuthorizationHandler extends AbstractFilterLogicHandler {

   private static final Logger LOG = LoggerFactory.getLogger(RequestAuthorizationHandler.class);
   private final OpenStackAuthenticationService authenticationService;
   private final EndpointListCache endpointListCache;
   private final ServiceEndpoint myEndpoint;

   public RequestAuthorizationHandler(OpenStackAuthenticationService authenticationService, EndpointListCache endpointListCache, ServiceEndpoint myEndpoint) {
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
         // Reject if no token
         LOG.debug("Authentication token not found in X-Auth-Token header. Rejecting request.");
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
      List<CachedEndpoint> cachedEndpoints = endpointListCache.getCachedEndpointsForToken(userToken);

      if (cachedEndpoints == null) {
         cachedEndpoints = requestEndpointsForTokenFromAuthService(userToken);

         try {
            endpointListCache.cacheEndpointsForToken(userToken, cachedEndpoints);
         } catch (IOException ioe) {
            LOG.error("Caching failure. Reason: " + ioe.getMessage(), ioe);
         }
      }

      return cachedEndpoints;
   }

   private List<CachedEndpoint> requestEndpointsForTokenFromAuthService(String userToken) {
      final List<Endpoint> authorizedEndpoints = authenticationService.getEndpointsForToken(userToken);
      final LinkedList<CachedEndpoint> serializable = new LinkedList<CachedEndpoint>();

      for (Endpoint ep : authorizedEndpoints) {
         serializable.add(new CachedEndpoint(ep.getPublicURL()));
      }

      return serializable;
   }

   // The X-Identity-Status header gets set if client authentication is in delegated mode.  If the token is valid, the
   // value of the X-Identity-Status header is "Confirmed".  If the token is not valid, then the X-Identity-Status
   // header is set to "Indeterminate".  In the future, we may want to allow authorization for delegated requests
   // that have a "Confirmed" status since we know the token is valid in that case.
   public boolean authenticationWasDelegated(HttpServletRequest request) {
      return StringUtilities.isNotBlank(request.getHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString()));
   }
}
