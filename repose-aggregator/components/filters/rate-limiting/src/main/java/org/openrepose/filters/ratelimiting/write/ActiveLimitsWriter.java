package org.openrepose.filters.ratelimiting.write;

import javax.ws.rs.core.MediaType;
import org.openrepose.filters.ratelimiting.exception.RateLimitingSerializationException;
import org.openrepose.filters.ratelimiting.util.LimitsEntityStreamTransformer;
import org.openrepose.services.ratelimit.config.Limits;
import org.openrepose.services.ratelimit.config.RateLimitList;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class ActiveLimitsWriter {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ActiveLimitsWriter.class);
   private static final LimitsEntityStreamTransformer RESPONSE_TRANSFORMER = new LimitsEntityStreamTransformer();

   public ActiveLimitsWriter() {
   }

   public MediaType write(RateLimitList activeRateLimits, MediaType mediaType, OutputStream outputStream) {

      try {
         final Limits limits = new Limits();
         limits.setRates(activeRateLimits);

         final ByteArrayOutputStream bos = new ByteArrayOutputStream();
         RESPONSE_TRANSFORMER.entityAsXml(limits, bos);

         final LimitsResponseMimeTypeWriter responseWriter = new LimitsResponseMimeTypeWriter(RESPONSE_TRANSFORMER);

         return responseWriter.writeLimitsResponse(bos.toByteArray(), mediaType, outputStream);
      } catch (Exception ex) {
         LOG.error("Failed to serialize limits upon user request. Reason: " + ex.getMessage(), ex);
         throw new RateLimitingSerializationException("Failed to serialize limits upon user request. Reason: " + ex.getMessage(), ex);
      }
   }
}
