package com.rackspace.papi.components.identity.content.auth;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderChooser;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.io.stream.LimitedReadInputStream;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.commons.util.http.header.QualityFactorHeaderChooser;
import com.rackspace.papi.commons.util.http.media.MediaRangeParser;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBElement;

import com.rackspacecloud.docs.auth.api.v1.Credentials;
import org.openrepose.rackspace.auth.content_identity.config.ContentIdentityAuthConfig;

public class ContentIdentityAuthHandler extends AbstractFilterLogicHandler {

   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ContentIdentityAuthHandler.class);
   private static final String DEFAULT_QUALITY = "0.6";
   private static final HeaderChooser<MediaType> CONTENT_TYPE_CHOOSER = new QualityFactorHeaderChooser<MediaType>(new MediaType(MimeType.APPLICATION_JSON, -1));
   private final Double quality;
   private final int streamLimit;
   private final ContentIdentityAuthConfig config;
   private final JacksonJaxbTransform jsonTransformer;
   private final Transform<InputStream, JAXBElement<Credentials>> xmlTransformer;

   public ContentIdentityAuthHandler(ContentIdentityAuthConfig config, JacksonJaxbTransform jsonTransformer, Transform<InputStream, JAXBElement<Credentials>> xmlTransformer) {
      this.config = config;
      this.jsonTransformer = jsonTransformer;
      this.xmlTransformer = xmlTransformer;
      this.quality = Double.valueOf(config.getQuality() != null ? config.getQuality() : DEFAULT_QUALITY);
      this.streamLimit = config.getContentBodyReadLimit().intValue();
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      filterDirector.setFilterAction(FilterAction.PASS);

      try {
         MimeType mimeType = CONTENT_TYPE_CHOOSER.choosePreferredHeaderValue(new MediaRangeParser(request.getHeaders("Content-Type")).parse()).getMimeType();
         InputStream inputStream = new LimitedReadInputStream(streamLimit, request.getInputStream());
         AuthCredentials credentials;

         try {
            inputStream.mark(streamLimit);
            credentials = new ContentParser(jsonTransformer, xmlTransformer).parse(mimeType, inputStream);

         } finally {
            inputStream.reset();
         }

         if (credentials != null && StringUtilities.isNotBlank(credentials.getId())) {
            headerManager.appendHeader(PowerApiHeader.USER.toString(), credentials.getId(), quality);
            headerManager.appendHeader(PowerApiHeader.GROUPS.toString(), config.getGroup(), quality);
         }

      } catch (IOException ex) {
         filterDirector.setFilterAction(FilterAction.NOT_SET);
         LOG.error("Unable to read message body stream. Reason: " + ex.getMessage(), ex);
      } catch (Exception ex) {
         filterDirector.setFilterAction(FilterAction.NOT_SET);
         LOG.error("Unable to read message body stream. Reason: " + ex.getMessage(), ex);
      }

      return filterDirector;
   }
}
