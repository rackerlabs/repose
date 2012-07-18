package com.rackspace.papi.components.ratelimit.write;

import javax.ws.rs.core.MediaType;
import com.rackspace.repose.service.limits.schema.Limits;
import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.repose.service.ratelimit.config.LimitsFormat;
import com.rackspace.papi.components.ratelimit.exception.RateLimitingSerializationException;
import com.rackspace.papi.components.ratelimit.util.LimitsEntityStreamTransformer;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class ActiveLimitsWriter {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ActiveLimitsWriter.class);
   private static final LimitsEntityStreamTransformer RESPONSE_TRANSFORMER = new LimitsEntityStreamTransformer();
   private final LimitsFormat limitsFormat;

   public ActiveLimitsWriter(LimitsFormat limitsFormat) {
      this.limitsFormat = limitsFormat;
   }

   public void write(RateLimitList activeRateLimits, MediaType mediaType, OutputStream outputStream) {

      try {
         final Limits limits = new Limits();
         limits.setRates(activeRateLimits);

         final ByteArrayOutputStream bos = new ByteArrayOutputStream();
         RESPONSE_TRANSFORMER.entityAsXml(limits, bos);

         final LimitsResponseMimeTypeWriter responseWriter = new LimitsResponseMimeTypeWriter(limitsFormat, RESPONSE_TRANSFORMER);
         responseWriter.writeLimitsResponse(bos.toByteArray(), mediaType, outputStream);
      } catch (Exception ex) {
         LOG.error("Failed to serialize limits upon user request. Reason: " + ex.getMessage(), ex);
         throw new RateLimitingSerializationException("Failed to serialize limits upon user request. Reason: " + ex.getMessage(), ex);
      }
   }
}
