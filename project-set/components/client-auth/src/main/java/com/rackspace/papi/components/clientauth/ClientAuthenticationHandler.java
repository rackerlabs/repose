/*
 *  Copyright 2010 Rackspace.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.commons.config.manager.LockedConfigurationUpdater;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import com.rackspace.papi.auth.AuthModule;

import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.rackspace.RackspaceAuthenticationModule;
import org.slf4j.Logger;
import com.rackspace.papi.filter.logic.FilterDirector;
import net.sf.ehcache.CacheManager;

/**
 *
 * @author jhopper
 */
public class ClientAuthenticationHandler extends AbstractFilterLogicHandler {

    private final KeyedStackLock updateLock = new KeyedStackLock();
    private final Object updateKey = new Object(), readKey = new Object();
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientAuthenticationHandler.class);
    private final CacheManager cacheManagerReference;
    private AuthModule authenticationModule;

    public ClientAuthenticationHandler(CacheManager cacheManager) {
        cacheManagerReference = cacheManager;
    }
    
    private final UpdateListener<ClientAuthConfig> clientAuthenticationConfigurationListener =
            new LockedConfigurationUpdater<ClientAuthConfig>(updateLock, updateKey) {

                @Override
                protected void onConfigurationUpdated(ClientAuthConfig modifiedConfig) {

                    /*
                     * Since this filter component is responseible for multiple
                     * auth schemes the configuration has different configured
                     * auth 'modules.'
                     */
                    if (modifiedConfig.getRackspaceAuth() != null) {
                        authenticationModule = new RackspaceAuthenticationModule(cacheManagerReference, modifiedConfig.getRackspaceAuth());
                    } else if (modifiedConfig.getHttpBasicAuth() != null) {
                    } else {
                        LOG.error("Authentication module is not understood or supported. Please check your configuration.");
                    }
                }
            };

    public UpdateListener<ClientAuthConfig> getClientAuthenticationConfigurationListener() {
        return clientAuthenticationConfigurationListener;
    }

    private AuthModule getCurrentAuthModule() {
        updateLock.lock(readKey);

        try {
            return authenticationModule;
        } finally {
            updateLock.unlock(readKey);
        }
    }

    private void updateHttpResponse(MutableHttpServletResponse httpResponse, String wwwAuthenticateHeader) {
        
        // If in the case that the origin service supports delegated authentication
        // we should then communicate to the client how to authenticate with us
        if (!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains("Delegated")) {
            final String replacementWwwAuthenticateHeader = getCurrentAuthModule().getWWWAuthenticateHeaderContents();
            httpResponse.setHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey(), replacementWwwAuthenticateHeader);
        } else {
            // In the case where authentication has failed and we did not recieve
            // a delegated WWW-Authenticate header, this means that our own authentication
            // with the origin service has failed and must then be communicated as
            // a 500 (internal service error) to the client
            httpResponse.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());
        }
    }

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        return getCurrentAuthModule().authenticate(request);
    }

    public void handleResponse(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        /// The WWW Authenticate header can be used to communicate to the client
        // (since we are a proxy) how to correctly authenticate itself
        final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey());

        switch (HttpStatusCode.fromInt(response.getStatus())) {
            // NOTE: We should only mutate the WWW-Authenticate header on a
            // 401 (unauthorized) or 403 (forbidden) response from the origin service
            case UNAUTHORIZED:
            case FORBIDDEN:
                updateHttpResponse(response, wwwAuthenticateHeader);
                break;
        }
    }
}
