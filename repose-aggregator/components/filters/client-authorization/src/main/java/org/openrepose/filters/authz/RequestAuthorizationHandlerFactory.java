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
package org.openrepose.filters.authz;

import org.openrepose.common.auth.AuthServiceException;
import org.openrepose.common.auth.openstack.AuthenticationService;
import org.openrepose.common.auth.openstack.AuthenticationServiceFactory;
import org.openrepose.commons.config.manager.UpdateFailedException;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.components.authz.rackspace.config.AuthenticationServer;
import org.openrepose.components.authz.rackspace.config.RackspaceAuthorization;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.filters.authz.cache.EndpointListCache;
import org.openrepose.filters.authz.cache.EndpointListCacheImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public class RequestAuthorizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RequestAuthorizationHandler> {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAuthorizationHandlerFactory.class);
    private final Datastore datastore;
    private RackspaceAuthorization authorizationConfiguration;
    private AuthenticationService authenticationService;
    private HttpClientService httpClientService;
    private AkkaServiceClient akkaServiceClient;

    public RequestAuthorizationHandlerFactory(Datastore datastore, HttpClientService httpClientService, AkkaServiceClient akkaServiceClient) {
        this.datastore = datastore;
        this.httpClientService = httpClientService;
        this.akkaServiceClient = akkaServiceClient;
    }

    @Override
    protected RequestAuthorizationHandler buildHandler() {

        if (!this.isInitialized()) {
            return null;
        }

        if (authenticationService == null) {
            LOG.error("Component has not been initialized yet. Please check your configurations.");
            throw new IllegalStateException("Component has not been initialized yet");
        }

        final EndpointListCache cache = new EndpointListCacheImpl(datastore, authorizationConfiguration.getAuthenticationServer().getEndpointListTtl());
        return new RequestAuthorizationHandler(authenticationService, cache, authorizationConfiguration.getServiceEndpoint(),
                authorizationConfiguration.getIgnoreTenantRoles(), authorizationConfiguration.getDelegating());
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
        updateListeners.put(RackspaceAuthorization.class, new RoutingConfigurationListener());

        return updateListeners;
    }

    private class RoutingConfigurationListener implements UpdateListener<RackspaceAuthorization> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(RackspaceAuthorization configurationObject) throws UpdateFailedException {
            authorizationConfiguration = configurationObject;

            final AuthenticationServer serverInfo = authorizationConfiguration.getAuthenticationServer();

            if (serverInfo != null && authorizationConfiguration.getServiceEndpoint() != null) {
                try {
                    authenticationService = new AuthenticationServiceFactory().build(serverInfo.getHref(),
                            serverInfo.getUsername(), serverInfo.getPassword(), serverInfo.getTenantId(),
                            configurationObject.getAuthenticationServer().getConnectionPoolId(), httpClientService,
                            akkaServiceClient);
                } catch (AuthServiceException e) {
                    throw new UpdateFailedException("Failed to authorize.", e);
                }
            } else {
                LOG.error("Errors detected in rackspace authorization configuration. Please check configurations.");
            }

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
