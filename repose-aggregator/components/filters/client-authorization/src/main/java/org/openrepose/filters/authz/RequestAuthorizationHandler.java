package org.openrepose.filters.authz;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.rackspace.httpdelegation.JavaDelegationManagerProxy;
import org.openrepose.common.auth.AuthServiceException;
import org.openrepose.common.auth.openstack.AuthenticationService;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.components.authz.rackspace.config.DelegatingType;
import org.openrepose.components.authz.rackspace.config.IgnoreTenantRoles;
import org.openrepose.components.authz.rackspace.config.ServiceEndpoint;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.filters.authz.cache.CachedEndpoint;
import org.openrepose.filters.authz.cache.EndpointListCache;
import org.openrepose.services.serviceclient.akka.AkkaServiceClientException;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class RequestAuthorizationHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAuthorizationHandler.class);
    private static final String CLIENT_AUTHORIZATION = "client-authorization";
    private final AuthenticationService authenticationService;
    private final EndpointListCache endpointListCache;
    private final ServiceEndpoint configuredEndpoint;
    private final DelegatingType delegating;
    private final List<String> ignoreTenantRoles;

    public RequestAuthorizationHandler(AuthenticationService authenticationService, EndpointListCache endpointListCache,
                                       ServiceEndpoint configuredEndpoint, IgnoreTenantRoles ignoreTenantRoles, DelegatingType delegating) {
        this.authenticationService = authenticationService;
        this.endpointListCache = endpointListCache;
        this.configuredEndpoint = configuredEndpoint;
        this.delegating = delegating;
        this.ignoreTenantRoles = getListOfRoles(ignoreTenantRoles);
    }

    private List<String> getListOfRoles(IgnoreTenantRoles ignoreTenantRoles) {
        List<String> roles = new ArrayList<>();
        if(ignoreTenantRoles != null) {
            roles.addAll(ignoreTenantRoles.getIgnoreTenantRole());
            roles.addAll(ignoreTenantRoles.getRole());
        }
        return roles;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.RETURN);
        myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String message = "Failure in authorization component";

        final String authenticationToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString());

        try {
            if (StringUtilities.isBlank(authenticationToken)) {
                // Reject if no token
                message = "Authentication token not found in X-Auth-Token header. Rejecting request.";
                LOG.debug(message);
                myDirector.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
            } else if (adminRoleMatchIgnoringCase(request.getHeaders(OpenStackServiceHeader.ROLES.toString())) ||
                            isEndpointAuthorizedForToken(authenticationToken)) {
                myDirector.setFilterAction(FilterAction.PASS);
            } else {
                message = "User token: " + authenticationToken +
                        ": The user's service catalog does not contain an endpoint that matches " +
                        "the endpoint configured in openstack-authorization.cfg.xml: \"" +
                        configuredEndpoint.getHref() + "\".  User not authorized to access service.";
                LOG.info(message);
                myDirector.setResponseStatusCode(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (AuthServiceException ex) {
            LOG.error(message);
            LOG.trace("", ex);
            if(ex.getCause() instanceof AkkaServiceClientException && ex.getCause().getCause() instanceof TimeoutException) {
                myDirector.setResponseStatusCode(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            } else {
                myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex) {
            LOG.error(message);
            LOG.trace("", ex);
            myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        if(delegating != null && myDirector.getFilterAction() != FilterAction.PASS) {
            myDirector.setFilterAction(FilterAction.PASS);
            for(Map.Entry<String, List<String>> mapHeaders : JavaDelegationManagerProxy.buildDelegationHeaders(myDirector.getResponseStatusCode(), CLIENT_AUTHORIZATION, message, delegating.getQuality()).entrySet()) {
                List<String> value = mapHeaders.getValue();
                myDirector.requestHeaderManager().appendHeader(mapHeaders.getKey(), value.toArray(new String[value.size()]));
            }
        }
        return myDirector;
    }

    private boolean adminRoleMatchIgnoringCase(Enumeration<String> roleStringList) {
        List<String> roles = Collections.list(roleStringList);
        if(!roles.isEmpty()) {
            for (String ignoreTenantRole : ignoreTenantRoles) {
                for (String role : roles) {
                    if (ignoreTenantRole.equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isEndpointAuthorizedForToken(String userToken) throws AuthServiceException {
        List<CachedEndpoint> cachedEndpoints = requestEndpointsForToken(userToken);
        if(cachedEndpoints != null) {
            return !Collections2.filter(cachedEndpoints, forMatchingEndpoint()).isEmpty();
        }
        return false;
    }

    Predicate<CachedEndpoint> forMatchingEndpoint() {
        return new Predicate<CachedEndpoint>() {
            @Override
            public boolean apply(CachedEndpoint input) {
                return matchesUrl(input.getPublicUrl()) && matchesRegion(input.getRegion()) &&
                       matchesName(input.getName()) && matchesType(input.getType());
            }

            private boolean matchesUrl(String publicUrl) {
                if (StringUtilities.isBlank(publicUrl)) {
                    LOG.warn("Endpoint Public URL is null.  This is a violation of the OpenStack Identity Service contract.");
                }
                return StringUtilities.nullSafeStartsWith(publicUrl, configuredEndpoint.getHref());
            }

            private boolean matchesRegion(String region) {
                return (configuredEndpoint.getRegion() == null) || configuredEndpoint.getRegion().equals(region);
            }

            private boolean matchesName(String name) {
                return (configuredEndpoint.getName() == null) || configuredEndpoint.getName().equals(name);
            }

            private boolean matchesType(String type) {
                if (StringUtilities.isBlank(type)) {
                    LOG.warn("Endpoint Type is null.  This is a violation of the OpenStack Identity Service contract.");
                }
                return (configuredEndpoint.getType() == null) || configuredEndpoint.getType().equals(type);
            }
        };
    }

    private List<CachedEndpoint> requestEndpointsForToken(String userToken) throws AuthServiceException {
        List<CachedEndpoint> cachedEndpoints = endpointListCache.getCachedEndpointsForToken(userToken);

        if (cachedEndpoints == null || cachedEndpoints.isEmpty()) {
            List<Endpoint> authorizedEndpoints = authenticationService.getEndpointsForToken(userToken);
            if(authorizedEndpoints != null) {
                cachedEndpoints = new LinkedList<>();
                for (Endpoint ep : authorizedEndpoints) {
                    cachedEndpoints.add(new CachedEndpoint(ep.getPublicURL(), ep.getRegion(), ep.getName(), ep.getType()));
                }
                try {
                    endpointListCache.cacheEndpointsForToken(userToken, cachedEndpoints);
                } catch (IOException ioe) {
                    LOG.error("Caching failure. Reason: " + ioe.getMessage(), ioe);
                }
            }
        }
        return cachedEndpoints;
    }
}
