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
package org.openrepose.filters.clientauth;

import org.openrepose.common.auth.AuthServiceException;
import org.openrepose.commons.config.manager.UpdateFailedException;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.ServiceClient;
import org.openrepose.commons.utils.regex.KeyedRegexExtractor;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.filters.clientauth.atomfeed.AuthFeedReader;
import org.openrepose.filters.clientauth.atomfeed.FeedListenerManager;
import org.openrepose.filters.clientauth.atomfeed.sax.SaxAuthFeedReader;
import org.openrepose.filters.clientauth.common.AuthenticationHandler;
import org.openrepose.filters.clientauth.common.UriMatcher;
import org.openrepose.filters.clientauth.config.ClientAuthConfig;
import org.openrepose.filters.clientauth.config.RackspaceIdentityFeed;
import org.openrepose.filters.clientauth.config.URIPattern;
import org.openrepose.filters.clientauth.config.WhiteList;
import org.openrepose.filters.clientauth.openstack.OpenStackAuthenticationHandlerFactory;
import org.openrepose.filters.clientauth.openstack.config.ClientMapping;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author jhopper
 *         <p/>
 *         The purpose of this class is to handle client authentication. Multiple
 *         authentication schemes may be used depending on the configuration. For
 *         example, a Rackspace specific or Basic Http authentication.
 */
@Deprecated
public class ClientAuthenticationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<AuthenticationHandler> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientAuthenticationHandlerFactory.class);
    private static final Long MINIMUM_INTERVAL = new Long("10000");
    private final Datastore datastore;
    private final HttpClientService httpClientService;
    private final String reposeVersion;
    private AuthenticationHandler authenticationModule;
    private KeyedRegexExtractor<String> accountRegexExtractor = new KeyedRegexExtractor<String>();
    private UriMatcher uriMatcher;
    private FeedListenerManager manager;
    private AkkaServiceClient akkaServiceClient;
    private boolean isOutboundTracing;


    public ClientAuthenticationHandlerFactory(Datastore datastore, HttpClientService httpClientService, AkkaServiceClient akkaServiceClient, String reposeVersion) {
        this.datastore = datastore;
        this.httpClientService = httpClientService;
        this.akkaServiceClient = akkaServiceClient;
        this.reposeVersion = reposeVersion;
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> listenerMap = new HashMap<Class, UpdateListener<?>>();
        listenerMap.put(ClientAuthConfig.class, new ClientAuthConfigurationListener());

        return listenerMap;
    }

    public void stopFeeds() {
        if (manager != null) {
            manager.stopReading();
        }
    }

    private void updateUriMatcher(WhiteList whiteList) {
        final List<Pattern> whiteListRegexPatterns = new ArrayList<Pattern>();

        if (whiteList != null) {
            for (URIPattern pattern : whiteList.getUriPattern()) {
                whiteListRegexPatterns.add(Pattern.compile(pattern.getUriRegex()));
            }
        }

        uriMatcher = new UriMatcher(whiteListRegexPatterns);
    }

    private AuthenticationHandler getOpenStackAuthHandler(ClientAuthConfig config) throws AuthServiceException {
        return OpenStackAuthenticationHandlerFactory.newInstance(config, accountRegexExtractor, datastore, uriMatcher, httpClientService, akkaServiceClient);
    }

    @Override
    protected AuthenticationHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return authenticationModule;
    }

    public void setOutboundTracing(boolean isOutboundTracing) {
        manager.setOutboundTracing(isOutboundTracing);
        this.isOutboundTracing = isOutboundTracing;
    }

    private class ClientAuthConfigurationListener implements UpdateListener<ClientAuthConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ClientAuthConfig modifiedConfig) throws UpdateFailedException {

            updateUriMatcher(modifiedConfig.getWhiteList());

            accountRegexExtractor.clear();
            if (modifiedConfig.getOpenstackAuth() != null) {
                try {
                    authenticationModule = getOpenStackAuthHandler(modifiedConfig);
                } catch (AuthServiceException e) {
                    throw new UpdateFailedException("Unable to retrieve OpenStack Auth Handler.", e);
                }
                for (ClientMapping clientMapping : modifiedConfig.getOpenstackAuth().getClientMapping()) {
                    accountRegexExtractor.addPattern(clientMapping.getIdRegex());
                }
                if (modifiedConfig.getAtomFeeds() != null) {
                    try {
                        activateOpenstackAtomFeedListener(modifiedConfig);
                    } catch (Exception e) {
                        throw new UpdateFailedException("Unable to activate OpenStack Atom Feed Listener.", e);
                    }
                } else if (manager != null) {
                    //Case where the user has an active feed manager, but has edited their config to not listen to atom feeds
                    manager.stopReading();
                }
            } else if (modifiedConfig.getHttpBasicAuth() != null) {
                // TODO: Create handler for HttpBasic
                authenticationModule = null;
            } else {
                LOG.error("Authentication module is not understood or supported. Please check your configuration.");
            }


            isInitialized = true;

        }

        //Launch listener for atom-feeds if config present
        private void activateOpenstackAtomFeedListener(ClientAuthConfig modifiedConfig) throws AuthServiceException {

            if (manager != null) {
                //If we have an existing manager we will shutdown the already running thread
                manager.stopReading();
            }
            List<AuthFeedReader> listeners = new ArrayList<AuthFeedReader>();

            for (RackspaceIdentityFeed feed : modifiedConfig.getAtomFeeds().getRsIdentityFeed()) {

                SaxAuthFeedReader rdr = new SaxAuthFeedReader(
                        new ServiceClient(modifiedConfig.getOpenstackAuth().getConnectionPoolId(), httpClientService),
                        akkaServiceClient,
                        reposeVersion,
                        feed.getUri(),
                        feed.getId(),
                        isOutboundTracing);

                //if the atom feed is authed, but no auth uri, user, and pass are configured we will use the same credentials we use for auth admin operations
                if (feed.isIsAuthed()) {
                    if (!StringUtilities.isBlank(feed.getAuthUri())) {
                        rdr.setAuthed(feed.getAuthUri(), feed.getUser(), feed.getPassword());
                    } else {
                        rdr.setAuthed(modifiedConfig.getOpenstackAuth().getIdentityService().getUri(), modifiedConfig.getOpenstackAuth().getIdentityService().getUsername(),
                                modifiedConfig.getOpenstackAuth().getIdentityService().getPassword());
                    }
                }
                listeners.add(rdr);
            }

            manager = new FeedListenerManager(datastore, listeners, getMinimumCheckInterval(modifiedConfig.getAtomFeeds().getCheckInterval()));
            manager.startReading();
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }

        //If the user configures repose to poll more often than every 10000 milliseconds (10 seconds) we will override this and set it to our minimum.
        private long getMinimumCheckInterval(long interval) {

            if (interval > MINIMUM_INTERVAL) {
                LOG.debug("Repose will poll Atom Feeds every " + MINIMUM_INTERVAL + " milliseconds for invalidated users and tokens");
                return interval;
            } else {
                LOG.warn("Configured minimum check interval to poll Atom Feeds set less than " + MINIMUM_INTERVAL + " milliseconds. To prevent flooding the atom endpoint with"
                        + " requests, Repose will poll the Atom Feeds every " + MINIMUM_INTERVAL + " instead.");
                return MINIMUM_INTERVAL;
            }
        }
    }
}
