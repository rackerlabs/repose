package org.openrepose.filters.authz;

import com.rackspace.auth.openstack.AuthenticationService;
import com.rackspace.auth.openstack.AuthenticationServiceFactory;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.services.datastore.Datastore;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.services.httpclient.HttpClientService;
import org.openrepose.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.components.authz.rackspace.config.AuthenticationServer;
import org.openrepose.components.authz.rackspace.config.RackspaceAuthorization;
import org.openrepose.filters.authz.cache.EndpointListCache;
import org.openrepose.filters.authz.cache.EndpointListCacheImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RequestAuthorizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RequestAuthorizationHandler> {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAuthorizationHandlerFactory.class);
    private final Datastore datastore;
    private RackspaceAuthorization authorizationConfiguration;
    private AuthenticationService authenticationService;
    private HttpClientService httpClientService;
    private AkkaServiceClient akkaServiceClient;

    public RequestAuthorizationHandlerFactory(Datastore datastore,HttpClientService httpClientService, AkkaServiceClient akkaServiceClient) {
        this.datastore = datastore;
        this.httpClientService = httpClientService;
        this.akkaServiceClient = akkaServiceClient;
    }

    private class RoutingConfigurationListener implements UpdateListener<RackspaceAuthorization> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(RackspaceAuthorization configurationObject) {
            authorizationConfiguration = configurationObject;

            final AuthenticationServer serverInfo = authorizationConfiguration.getAuthenticationServer();

            if (serverInfo != null && authorizationConfiguration.getServiceEndpoint() != null) {
                authenticationService = new AuthenticationServiceFactory().build(serverInfo.getHref(),
                        serverInfo.getUsername(), serverInfo.getPassword(), serverInfo.getTenantId(),
                        configurationObject.getAuthenticationServer().getConnectionPoolId(),httpClientService,
                        akkaServiceClient);
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
                authorizationConfiguration.getIgnoreTenantRoles());
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
        updateListeners.put(RackspaceAuthorization.class, new RoutingConfigurationListener());

        return updateListeners;
    }
}
