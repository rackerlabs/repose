/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.clientauth.openstack;

import org.openrepose.common.auth.AuthServiceException;
import org.openrepose.common.auth.openstack.AuthenticationService;
import org.openrepose.common.auth.openstack.AuthenticationServiceFactory;
import org.openrepose.commons.utils.regex.KeyedRegexExtractor;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.filters.clientauth.common.*;
import org.openrepose.filters.clientauth.config.ClientAuthConfig;
import org.openrepose.filters.clientauth.openstack.config.IgnoreTenantRoles;
import org.openrepose.filters.clientauth.openstack.config.OpenStackIdentityService;
import org.openrepose.filters.clientauth.openstack.config.OpenstackAuth;
import org.openrepose.filters.clientauth.openstack.config.ServiceAdminRoles;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public final class OpenStackAuthenticationHandlerFactory {

    private OpenStackAuthenticationHandlerFactory() {
    }

    public static AuthenticationHandler newInstance(ClientAuthConfig config, KeyedRegexExtractor accountRegexExtractor,
                                                    Datastore datastore, UriMatcher uriMatcher,
                                                    HttpClientService httpClientService, AkkaServiceClient akkaServiceClient) throws AuthServiceException {
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

        final Configurables configurables = new Configurables(config.getDelegating() != null,
                config.getDelegating() != null ? config.getDelegating().getQuality() : 0.0,
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
                authConfig.isSendAllTenantIds(),
                authConfig.isSendTenantIdQuality());

        return new OpenStackAuthenticationHandler(configurables, authService, cache, grpCache, usrCache, endpointsCache, uriMatcher);
    }

    private static List<String> getServiceAdminRoles(ServiceAdminRoles roles) {
        return roles == null ? new ArrayList<String>() : roles.getRole();
    }

    private static List<String> getIgnoreTenantRoles(IgnoreTenantRoles roles) {
        if (roles == null) {
            return new ArrayList<String>();
        } else {
            return roles.getRole();
        }
    }
}
