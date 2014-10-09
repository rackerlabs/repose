package org.openrepose.rackspace.auth_2_0.identity.content.auth;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.media.MediaRangeProcessor;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.io.stream.LimitedReadInputStream;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.commons.utils.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.rackspace.auth2.content_identity.config.ContentIdentityAuthConfig;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;

public class ContentIdentityAuthHandler extends AbstractFilterLogicHandler {
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ContentIdentityAuthHandler.class);

   private static final String DEFAULT_QUALITY = "0.6";
   private static final MediaType DEFAULT_TYPE = new MediaType(MimeType.APPLICATION_JSON);
   private final Double quality;
   private final int streamLimit;
   private final ContentIdentityAuthConfig config;
   private final JacksonJaxbTransform jsonTransformer;
   private final Unmarshaller unmarshaller;

   public ContentIdentityAuthHandler(ContentIdentityAuthConfig config, JacksonJaxbTransform jsonTransformer, Unmarshaller unmarshaller) {
      this.config = config;
      this.jsonTransformer = jsonTransformer;
      this.unmarshaller = unmarshaller;
      this.quality = Double.valueOf(config.getQuality() != null ? config.getQuality() : DEFAULT_QUALITY);
      this.streamLimit = config.getContentBodyReadLimit().intValue();
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      filterDirector.setFilterAction(FilterAction.PASS);
      MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
      MediaRangeProcessor processor = new MediaRangeProcessor(mutableRequest.getPreferredHeaderValues("Content-Type", DEFAULT_TYPE));

      try {
         MediaType mediaType = processor.process().get(0);
         MimeType mimeType = mediaType.getMimeType();
         InputStream inputStream = new LimitedReadInputStream(streamLimit, request.getInputStream());
         AuthCredentials credentials;

         try {
            inputStream.mark(streamLimit);
            credentials = new ContentParser(jsonTransformer, unmarshaller).parse(mimeType, inputStream);

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
