package com.rackspace.papi.components.identity.content.auth;

import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.components.identity.content.wrappers.CredentialsWrapper;
import com.rackspace.papi.components.identity.parsers.JsonAuthBodyParser;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.openrepose.rackspace.auth.content_identity.config.ContentIdentityAuthConfig;


public class ContentIdentityAuthHandler extends AbstractFilterLogicHandler {

   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ContentIdentityAuthHandler.class);
   private final ContentIdentityAuthConfig config;
   private final JacksonJaxbTransform jsonTranformer;

   public ContentIdentityAuthHandler(ContentIdentityAuthConfig config, JacksonJaxbTransform jsonTranformer) {
      this.config = config;
      this.jsonTranformer = jsonTranformer;
   }
   
   private boolean isJson(MimeType mimeType) {
      return mimeType != null && mimeType.APPLICATION_JSON.getSubType().equals(mimeType.getSubType());
   }
   
   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      
      final FilterDirector filterDirector = new FilterDirectorImpl();
      filterDirector.setFilterAction(FilterAction.PASS);
      
      try {
         String contentType = request.getHeader("Content-Type");

         MimeType mimeType = MimeType.guessMediaTypeFromString(contentType);
         CredentialsWrapper credentials;

         if (isJson(mimeType)) {
               credentials = new JsonAuthBodyParser(jsonTranformer).parse(request.getInputStream());
         } else {
         }

         // TODO: Do Magic Here
      } catch (IOException ex) {
         filterDirector.setFilterAction(FilterAction.NOT_SET);
         LOG.error("Unable to read message body stream", ex);
      }
      
      return filterDirector;
   }
}
