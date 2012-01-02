package com.rackspace.papi.components.clientauth;

import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.auth.v1_1.AuthenticationServiceClient;
import com.rackspace.auth.v1_1.AuthenticationServiceClientFactory;
import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.commons.config.manager.UpdateListener;

import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.openstack.config.OpenStackIdentityService;
import com.rackspace.papi.components.clientauth.openstack.config.OpenstackAuth;
import com.rackspace.papi.components.clientauth.openstack.config.ClientMapping;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

/**
 *
 * @author jhopper
 *
 * The purpose of this class is to handle client authentication. Multiple
 * authentication schemes may be used depending on the configuration. For
 * example, a Rackspace specific or Basic Http authentication.
 *
 */
public class ClientAuthenticationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<AuthModule> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientAuthenticationHandlerFactory.class);
    private AuthModule authenticationModule;
    private KeyedRegexExtractor<String> accountRegexExtractor = new KeyedRegexExtractor<String>();

    public ClientAuthenticationHandlerFactory() {
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> listenerMap = new HashMap<Class, UpdateListener<?>>();
        listenerMap.put(ClientAuthConfig.class, new ClientAuthConfigurationListener());

        return listenerMap;
    }

    private class ClientAuthConfigurationListener implements UpdateListener<ClientAuthConfig> {

        @Override
        public void configurationUpdated(ClientAuthConfig modifiedConfig) {

            if (modifiedConfig.getRackspaceAuth() != null) {
                authenticationModule = getAuth1_1Handler(modifiedConfig);
                for (AccountMapping accountMapping : modifiedConfig.getRackspaceAuth().getAccountMapping()) {
                    accountRegexExtractor.addPattern(accountMapping.getIdRegex(), accountMapping.getType().value());
                }
            } else if (modifiedConfig.getOpenstackAuth() != null) {
                authenticationModule = getOpenStackAuthHandler(modifiedConfig);
                for (ClientMapping clientMapping : modifiedConfig.getOpenstackAuth().getClientMapping()) {
                    accountRegexExtractor.addPattern(clientMapping.getIdRegex());
                }
            } else if (modifiedConfig.getHttpBasicAuth() != null) {
                // TODO: Create handler for HttpBasic
                authenticationModule = null;
            } else {
                LOG.error("Authentication module is not understood or supported. Please check your configuration.");
            }
        }
    }

    private AuthModule getAuth1_1Handler(ClientAuthConfig cfg) {
        final RackspaceAuth authConfig = cfg.getRackspaceAuth();

        final AuthenticationServiceClient serviceClient = new AuthenticationServiceClientFactory().buildAuthServiceClient(
                authConfig.getAuthenticationServer().getUri(), authConfig.getAuthenticationServer().getUsername(), authConfig.getAuthenticationServer().getPassword());
        return new com.rackspace.papi.components.clientauth.rackspace.v1_1.RackspaceAuthenticationHandler(authConfig, serviceClient, accountRegexExtractor);
    }

    private AuthModule getOpenStackAuthHandler(ClientAuthConfig config) {
        final OpenstackAuth authConfig = config.getOpenstackAuth();
        final OpenStackIdentityService ids = authConfig.getIdentityService();

        final OpenStackAuthenticationService authService = new com.rackspace.auth.openstack.ids.AuthenticationServiceClient(ids.getUri(), ids.getUsername(), ids.getPassword());
        return new com.rackspace.papi.components.clientauth.openstack.v1_0.OpenStackAuthenticationHandler(authConfig, authService, accountRegexExtractor);
    }

    @Override
    protected AuthModule buildHandler() {
        return authenticationModule;
    }
}
