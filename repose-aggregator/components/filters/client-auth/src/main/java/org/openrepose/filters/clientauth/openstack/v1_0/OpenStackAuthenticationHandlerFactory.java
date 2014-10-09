package org.openrepose.filters.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.AuthenticationService;
import com.rackspace.auth.openstack.AuthenticationServiceFactory;
import org.openrepose.commons.utils.regex.KeyedRegexExtractor;
import org.openrepose.filters.clientauth.common.AuthGroupCache;
import org.openrepose.filters.clientauth.common.AuthTokenCache;
import org.openrepose.filters.clientauth.common.AuthUserCache;
import org.openrepose.filters.clientauth.common.AuthenticationHandler;
import org.openrepose.filters.clientauth.common.Configurables;
import org.openrepose.filters.clientauth.common.EndpointsCache;
import org.openrepose.filters.clientauth.common.EndpointsConfiguration;
import org.openrepose.filters.clientauth.common.UriMatcher;
import org.openrepose.filters.clientauth.config.ClientAuthConfig;
import org.openrepose.filters.clientauth.openstack.config.IgnoreTenantRoles;
import org.openrepose.filters.clientauth.openstack.config.OpenStackIdentityService;
import org.openrepose.filters.clientauth.openstack.config.OpenstackAuth;
import org.openrepose.filters.clientauth.openstack.config.ServiceAdminRoles;
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient;
import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.service.httpclient.HttpClientService;

import java.util.ArrayList;
import java.util.List;

public final class OpenStackAuthenticationHandlerFactory {

    private OpenStackAuthenticationHandlerFactory() {
    }

    public static AuthenticationHandler newInstance(ClientAuthConfig config, KeyedRegexExtractor accountRegexExtractor,
                                                    Datastore datastore, UriMatcher uriMatcher,
                                                    HttpClientService httpClientService, AkkaServiceClient akkaServiceClient) {
        final AuthTokenCache cache = new AuthTokenCache(datastore, OsAuthCachePrefix.TOKEN.toString());
        final AuthGroupCache grpCache = new AuthGroupCache(datastore, OsAuthCachePrefix.GROUP.toString());
        final AuthUserCache usrCache = new AuthUserCache(datastore, OsAuthCachePrefix.USER.toString());
        final EndpointsCache endpointsCache = new EndpointsCache(datastore, OsAuthCachePrefix.USER.toString());
        final OpenstackAuth authConfig = config.getOpenstackAuth();
        final OpenStackIdentityService ids = authConfig.getIdentityService();
        final EndpointsConfiguration endpointsConfiguration;
        final AuthenticationService authService = new AuthenticationServiceFactory().build(ids.getUri(),
                                                                                           ids.getUsername(),
                                                                                           ids.getPassword(),
                                                                                           ids.getTenantId(),
                                                                                           authConfig.getConnectionPoolId(),
                                                                                           httpClientService,
                akkaServiceClient);

        //null check to prevent NPE when accessing config element attributes
        if (authConfig.getEndpointsInHeader() != null) {
            endpointsConfiguration = new EndpointsConfiguration(authConfig.getEndpointsInHeader().getFormat().toString(),
                                                                authConfig.getEndpointsInHeader().getCacheTimeout(),
                                                                authConfig.getEndpointsInHeader()
                                                                        .getIdentityContractVersion().intValue());
        } else {
            endpointsConfiguration = null;
        }

        final Configurables configurables = new Configurables(authConfig.isDelegable(),
                ids.getUri(),
                accountRegexExtractor,
                authConfig.isTenanted(), 
                authConfig.getGroupCacheTimeout(),
                authConfig.getTokenCacheTimeout(),
                authConfig.getUserCacheTimeout(),
                authConfig.getCacheOffset(),
                authConfig.isRequestGroups(),
                endpointsConfiguration,
                getServiceAdminRoles(authConfig.getServiceAdminRoles()),
                getIgnoreTenantRoles(authConfig.getIgnoreTenantRoles()),
                authConfig.isSendAllTenantIds());

        return new OpenStackAuthenticationHandler(configurables, authService, cache, grpCache, usrCache, endpointsCache, uriMatcher);
    }

    private static List<String> getServiceAdminRoles(ServiceAdminRoles roles){
        return roles == null ? new ArrayList<String>() : roles.getRole();
    }

    private static List<String> getIgnoreTenantRoles(IgnoreTenantRoles roles){
        if(roles == null) {
            return new ArrayList<String>();
        } else {
            return roles.getRole();
        }
    }
}
