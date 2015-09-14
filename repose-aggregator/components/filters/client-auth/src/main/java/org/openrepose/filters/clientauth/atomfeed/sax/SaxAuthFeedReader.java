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
package org.openrepose.filters.clientauth.atomfeed.sax;

import org.openrepose.common.auth.AuthServiceException;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.ServiceClient;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.logging.TracingHeaderHelper;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.filters.clientauth.atomfeed.*;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/*
 * Simple Atom Feed reader using Jersey + Sax Parser specifically for RS Identity Feed
 */
@Deprecated
public class SaxAuthFeedReader extends DefaultHandler implements AuthFeedReader {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SaxAuthFeedReader.class);
    private final String feedId;
    private final String feedHead;
    private String targetFeed;
    private String curResource;
    private final ServiceClient client;
    private boolean moreData;
    private CacheKeys resultKeys;
    private SAXParserFactory factory;
    private CacheKeyType curType;
    /*
     * IF the atom feed is authed,
     * THEN we have to provide an admin token with the request.
     */
    private boolean isAuthed = false;
    private String adminToken;
    private AdminTokenProvider provider;
    private String user;

    private final AkkaServiceClient akkaServiceClient;
    private final String reposeVersion;
    private boolean isOutboundTracing = false;

    public SaxAuthFeedReader(ServiceClient client, AkkaServiceClient akkaClient, String reposeVersion, String feedHead, String feedId, boolean isOutboundTracing) {
        this.client = client;
        this.feedHead = feedHead;
        this.targetFeed = feedHead;
        this.feedId = feedId;
        this.akkaServiceClient = akkaClient;
        this.reposeVersion = reposeVersion;
        this.isOutboundTracing = isOutboundTracing;
        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
    }

    public void setOutboundTracing(boolean isOutboundTracing) {
        this.isOutboundTracing = isOutboundTracing;
    }

    public void setAuthed(String uri, String user, String pass) throws AuthServiceException {
        isAuthed = true;
        this.user = user;
        provider = new AdminTokenProvider(akkaServiceClient, uri, user, pass);
        adminToken = provider.getAdminToken();
    }

    @Override
    public CacheKeys getCacheKeys(String traceID) throws FeedException {

        moreData = true;
        ServiceClientResponse resp;
        resultKeys = new FeedCacheKeys();
        while (moreData) {

            resp = getFeed(traceID);

            if (resp.getStatus() == HttpServletResponse.SC_OK) {

                try {
                    SAXParser parser = factory.newSAXParser();

                    parser.parse(resp.getData(), this);
                } catch (ParserConfigurationException ex) {
                    LOG.error("Error configuring SAXPARSER", ex);
                } catch (SAXException ex) {
                    LOG.error("Error within SAXPARSER", ex);
                } catch (IOException ex) {
                    LOG.error("Error reading response from atom feed", ex);
                }
            }
        }

        return resultKeys;
    }

    private ServiceClientResponse getFeed(String traceID) throws FeedException {

        ServiceClientResponse resp;
        final Map<String, String> headers = new HashMap<>();

        if (isOutboundTracing) {
            headers.put(CommonHttpHeader.TRACE_GUID.toString(),
                    TracingHeaderHelper.createTracingHeader(traceID, "1.1 Repose (Repose/" + reposeVersion + ")", user));
        }

        if (isAuthed) {
            headers.put(CommonHttpHeader.AUTH_TOKEN.toString(), adminToken);
        }
        resp = client.get(targetFeed, headers);

        switch (resp.getStatus()) {
            case HttpServletResponse.SC_OK:
                break;
            case HttpServletResponse.SC_UNAUTHORIZED:
                if (isAuthed) {
                    try {
                        adminToken = provider.getFreshAdminToken();
                    } catch (AuthServiceException e) {
                        throw new FeedException("Failed to obtain credentials.", e);
                    }
                    if (isOutboundTracing) {
                        headers.put(CommonHttpHeader.TRACE_GUID.toString(),
                                TracingHeaderHelper.createTracingHeader(traceID, "1.1 Repose (Repose/" + reposeVersion + ")", user));
                    }
                    headers.put(CommonHttpHeader.AUTH_TOKEN.toString(), adminToken);
                    resp = client.get(targetFeed, headers);
                } else { // case where we're getting back 401s and the client has not configured auth credentials for this feed.
                    LOG.warn("Feed at " + targetFeed + " requires Authentication. Please reconfigure Feed " + feedId + " with valid credentials and/or configure "
                            + "isAuthed to true");
                    moreData = false;
                }
                break;
            case HttpServletResponse.SC_NOT_FOUND:
                LOG.warn("Feed " + feedId + " not found at: " + targetFeed + "\nResetting feed target to: " + feedHead);
                targetFeed = feedHead;
                moreData = false;
                break;
            default: // If we receive anything other than a 200 or a 401 there is an error with the atom feed
                LOG.warn("Unable to retrieve atom feed from Feed" + feedId + ": " + targetFeed + "\n Response Code: " + resp.getStatus());
                moreData = false;
                break;

        }
        return resp;
    }

    @Override
    public void startDocument() {

        moreData = false;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (StringUtilities.nullSafeEquals(uri, "http://www.w3.org/2005/Atom") && StringUtilities.nullSafeEquals(localName, "link")) {
            // Get Prev Link
            if ("previous".equals(attributes.getValue("rel"))) {
                targetFeed = attributes.getValue("href");
                moreData = true; // There is a previous link to follow so we will continue to request atom feeds
            }
        }

        if (StringUtilities.nullSafeEquals(uri, "http://docs.rackspace.com/core/event") && StringUtilities.nullSafeEquals(localName, "event")) {
            //service code and resource type
            curResource = attributes.getValue("resourceId");
        }

        if (StringUtilities.nullSafeEquals(localName, "product") && StringUtilities.isNotBlank(curResource)) {
            if (StringUtilities.nullSafeEquals(uri, "http://docs.rackspace.com/event/identity/user")
                    && StringUtilities.nullSafeEquals(attributes.getValue("resourceType"), "USER")) {
                curType = CacheKeyType.USER;
            } else if (StringUtilities.nullSafeEquals(uri, "http://docs.rackspace.com/event/identity/token")
                    && StringUtilities.nullSafeEquals(attributes.getValue("resourceType"), "TOKEN")) {
                curType = CacheKeyType.TOKEN;
            } else if (StringUtilities.nullSafeEquals(uri, "http://docs.rackspace.com/event/identity/trr/user")
                    && StringUtilities.nullSafeEquals(attributes.getValue("resourceType"), "TRR_USER")) {
                curType = CacheKeyType.TRR_USER;
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (StringUtilities.nullSafeEquals(localName, "event")) {
            switch (curType) {
                case TOKEN:
                    resultKeys.addTokenKey(curResource);
                    break;
                case USER:
                    resultKeys.addUserKey(curResource);
                    break;
                case TRR_USER:
                    resultKeys.addUserKey(curResource);
                    break;
            }
            curResource = "";
        }
    }
}
