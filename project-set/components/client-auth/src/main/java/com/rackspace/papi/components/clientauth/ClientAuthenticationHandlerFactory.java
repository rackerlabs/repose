package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.components.clientauth.atomfeed.FeedListenerManager;
import com.rackspace.papi.components.clientauth.atomfeed.AuthFeedReader;
import com.rackspace.papi.components.clientauth.atomfeed.sax.SaxAuthFeedReader;
import com.rackspace.papi.components.clientauth.common.AuthenticationHandler;
import com.rackspace.papi.components.clientauth.common.UriMatcher;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.config.RackspaceIdentityFeed;
import com.rackspace.papi.components.clientauth.config.URIPattern;
import com.rackspace.papi.components.clientauth.config.WhiteList;
import com.rackspace.papi.components.clientauth.openstack.config.ClientMapping;
import com.rackspace.papi.components.clientauth.openstack.v1_0.OpenStackAuthenticationHandlerFactory;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;
import com.rackspace.papi.components.clientauth.rackspace.v1_1.RackspaceAuthenticationHandlerFactory;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.service.datastore.Datastore;
import org.slf4j.Logger;

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
public class ClientAuthenticationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<AuthenticationHandler> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientAuthenticationHandlerFactory.class);
    private AuthenticationHandler authenticationModule;
    private KeyedRegexExtractor<String> accountRegexExtractor = new KeyedRegexExtractor<String>();
    private UriMatcher uriMatcher;
    private final Datastore datastore;
    private FeedListenerManager manager;
    private static final Long minInterval = new Long("10000");

    public ClientAuthenticationHandlerFactory(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> listenerMap = new HashMap<Class, UpdateListener<?>>();
        listenerMap.put(ClientAuthConfig.class, new ClientAuthConfigurationListener());

        return listenerMap;
    }

    private class ClientAuthConfigurationListener implements UpdateListener<ClientAuthConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ClientAuthConfig modifiedConfig) {

            updateUriMatcher(modifiedConfig.getWhiteList());

            accountRegexExtractor.clear();
            if (modifiedConfig.getRackspaceAuth() != null) {
                authenticationModule = getRackspaceAuthHandler(modifiedConfig);
                for (AccountMapping accountMapping : modifiedConfig.getRackspaceAuth().getAccountMapping()) {
                    accountRegexExtractor.addPattern(accountMapping.getIdRegex(), accountMapping.getType().value());
                }
            } else if (modifiedConfig.getOpenstackAuth() != null) {
                authenticationModule = getOpenStackAuthHandler(modifiedConfig);
                for (ClientMapping clientMapping : modifiedConfig.getOpenstackAuth().getClientMapping()) {
                    accountRegexExtractor.addPattern(clientMapping.getIdRegex());
                }
                if (modifiedConfig.getAtomFeeds() != null) {
                    activateOpenstackAtomFeedListener(modifiedConfig);
                } else if (manager != null) { //Case where the user has an active feed manager, but has edited their config to not listen to atom feeds
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
        private void activateOpenstackAtomFeedListener(ClientAuthConfig modifiedConfig) {

            if (manager != null) { //If we have an existing manager we will shutdown the already running thread
                manager.stopReading();
            }
            List<AuthFeedReader> listeners = new ArrayList<AuthFeedReader>();

            for (RackspaceIdentityFeed feed : modifiedConfig.getAtomFeeds().getRsIdentityFeed()) {
                SaxAuthFeedReader rdr = new SaxAuthFeedReader(new ServiceClient(), feed.getUri(), feed.getId());

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

            if (interval > minInterval) {
                LOG.debug("Repose will poll Atom Feeds every " + minInterval + " milliseconds for invalidated users and tokens");
                return interval;
            } else {
                LOG.warn("Configured minimum check interval to poll Atom Feeds set less than " + minInterval + " milliseconds. To prevent flooding the atom endpoint with"
                        + " requests, Repose will poll the Atom Feeds every " + minInterval + " instead.");
                return minInterval;
            }
        }
    }

    public void stopFeeds() {
        if(manager != null){
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

    private AuthenticationHandler getRackspaceAuthHandler(ClientAuthConfig cfg) {
        return RackspaceAuthenticationHandlerFactory.newInstance(cfg, accountRegexExtractor, datastore, uriMatcher);
    }

    private AuthenticationHandler getOpenStackAuthHandler(ClientAuthConfig config) {
        return OpenStackAuthenticationHandlerFactory.newInstance(config, accountRegexExtractor, datastore, uriMatcher);
    }

    @Override
    protected AuthenticationHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return authenticationModule;
    }
}
