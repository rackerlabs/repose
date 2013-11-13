/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.clientauth.atomfeed.sax;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import static com.rackspace.papi.commons.util.http.HttpStatusCode.OK;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.components.clientauth.atomfeed.AuthFeedReader;
import com.rackspace.papi.components.clientauth.atomfeed.CacheKeyType;
import com.rackspace.papi.components.clientauth.atomfeed.CacheKeys;
import com.rackspace.papi.components.clientauth.atomfeed.FeedCacheKeys;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
   private boolean isAuthed = false; //If the atom feed is authed then we have to provide an admin token with the request;
   private String adminToken;
   private AdminTokenProvider provider;
   private String feedId;

   public SaxAuthFeedReader(ServiceClient client, String targetFeed, String feedId) {
      this.client = client;
      this.targetFeed = targetFeed;
      this.feedId = feedId;
      factory = SAXParserFactory.newInstance();
      factory.setNamespaceAware(true);
   }

   public void setAuthed(String uri, String user, String pass) {
      isAuthed = true;
      provider = new AdminTokenProvider(client, uri, user, pass);
      adminToken = provider.getAdminToken();
   }

   @Override
   public CacheKeys getCacheKeys() {

      moreData = true;
      ServiceClientResponse resp;
      resultKeys = new FeedCacheKeys();
      while (moreData) {

         resp = getFeed();

         if (resp.getStatusCode() == HttpStatusCode.OK.intValue()) {

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

   private ServiceClientResponse getFeed() {

      ServiceClientResponse resp;
      final Map<String, String> headers = new HashMap<String, String>();

      if (isAuthed) {
         headers.put(CommonHttpHeader.AUTH_TOKEN.toString(), adminToken);
      }
      resp = client.get(targetFeed, headers);


      switch (HttpStatusCode.fromInt(resp.getStatusCode())) {
         case OK:
            break;
         case UNAUTHORIZED:
            if (isAuthed) {
               adminToken = provider.getFreshAdminToken();
               headers.put(CommonHttpHeader.AUTH_TOKEN.toString(), adminToken);
               resp = client.get(targetFeed, headers);
            }else{ // case where we're getting back 401s and the client has not configured auth credentials for this feed.
               LOG.warn("Feed at " + targetFeed + " requires Authentication. Please reconfigure Feed " + feedId + " with valid credentials and/or configure "
                       + "isAuthed to true");
               moreData = false;
            }
            break;
         default: // If we receive anything other than a 200 or a 401 there is an error with the atom feed
            LOG.warn("Unable to retrieve atom feed from Feed" + feedId + ": " + targetFeed + "\n Response Code: " + resp.getStatusCode());
            moreData = false;

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
                 && StringUtilities.nullSafeEquals(attributes.getValue("resourceType"), ("USER"))) {
            curType = CacheKeyType.USER;
         } else if (StringUtilities.nullSafeEquals(uri, "http://docs.rackspace.com/event/identity/token")
                 && StringUtilities.nullSafeEquals(attributes.getValue("resourceType"), ("TOKEN"))) {
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

   @Override
   public String getFeedId() {
      return feedId;
   }
}
