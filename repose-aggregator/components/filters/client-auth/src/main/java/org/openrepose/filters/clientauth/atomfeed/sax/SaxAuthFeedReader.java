/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openrepose.filters.clientauth.atomfeed.sax;

import org.openrepose.common.auth.AuthServiceException;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.ServiceClient;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.filters.clientauth.atomfeed.*;
import org.openrepose.services.serviceclient.akka.AkkaServiceClient;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Simple Atom Feed reader using Jersey + Sax Parser specifically for RS Identity Feed
 */
public class SaxAuthFeedReader extends DefaultHandler implements AuthFeedReader {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SaxAuthFeedReader.class);
    private ServiceClient client;
    private String targetFeed;
    private String curResource;
    List<String> cacheKeys = new ArrayList<String>();
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
    private String feedId;

    private AkkaServiceClient akkaServiceClient;

    public SaxAuthFeedReader(ServiceClient client, AkkaServiceClient akkaClient, String targetFeed, String feedId) {
        this.client = client;
        this.targetFeed = targetFeed;
        this.feedId = feedId;
        this.akkaServiceClient = akkaClient;
        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
    }

    public void setAuthed(String uri, String user, String pass) throws AuthServiceException {
        isAuthed = true;
        provider = new AdminTokenProvider(akkaServiceClient, uri, user, pass);
        adminToken = provider.getAdminToken();
    }

    @Override
    public CacheKeys getCacheKeys() throws FeedException {

        moreData = true;
        ServiceClientResponse resp;
        resultKeys = new FeedCacheKeys();
        while (moreData) {

            resp = getFeed();

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

    private ServiceClientResponse getFeed() throws FeedException {

        ServiceClientResponse resp;
        final Map<String, String> headers = new HashMap<String, String>();

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
                    headers.put(CommonHttpHeader.AUTH_TOKEN.toString(), adminToken);
                    resp = client.get(targetFeed, headers);
                } else { // case where we're getting back 401s and the client has not configured auth credentials for this feed.
                    LOG.warn("Feed at " + targetFeed + " requires Authentication. Please reconfigure Feed " + feedId + " with valid credentials and/or configure "
                            + "isAuthed to true");
                    moreData = false;
                }
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
            }
            curResource = "";
        }
    }
}
