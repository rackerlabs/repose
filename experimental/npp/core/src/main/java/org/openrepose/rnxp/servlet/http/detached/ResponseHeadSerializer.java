package org.openrepose.rnxp.servlet.http.detached;

import java.util.Iterator;
import javax.servlet.http.HttpServletResponse;
import org.jboss.netty.buffer.ChannelBuffer;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.io.control.HttpMessageSerializer;
import org.openrepose.rnxp.http.util.StringCharsetEncoder;

import static org.jboss.netty.buffer.ChannelBuffers.*;
import static org.openrepose.rnxp.http.io.control.HttpControlSequence.*;

/**
 *
 * @author zinic
 */
public class ResponseHeadSerializer implements HttpMessageSerializer {

   private static final StringCharsetEncoder ASCII_ENCODER = StringCharsetEncoder.asciiEncoder();
   private final HttpServletResponse response;
   private final ChannelBuffer buffer;
   private byte[] currentHeaderKeyBytes;
   private Iterator<String> headerKeysRemaining;
   private Iterator<String> headerValuesRemaining;
   private HttpMessageComponent nextComponent;

   public ResponseHeadSerializer(HttpServletResponse response) {
      this.response = response;

      nextComponent = HttpMessageComponent.HTTP_VERSION;
      buffer = buffer(16384);
   }

   @Override
   public int read() {
      try {
         if (!buffer.readable()) {
            if (loadNextComponent()) {
               return -1;
            }
         }

         return buffer.readByte();
      } catch (Exception ex) {
         ex.printStackTrace();
         return -1;
      }
   }

   public boolean loadNextComponent() {
      switch (nextComponent) {
         case HTTP_VERSION:
            buffer.writeBytes(HTTP_VERSION.getBytes());
            buffer.writeBytes(SPACE.getBytes());

            nextComponent = HttpMessageComponent.RESPONSE_STATUS_CODE;
            break;

         case RESPONSE_STATUS_CODE:
            buffer.writeBytes(ASCII_ENCODER.encode(response.getStatus()));
            buffer.writeBytes(SPACE.getBytes());
            buffer.writeBytes(LINE_END.getBytes());

            nextComponent = HttpMessageComponent.HEADER;
            break;

         case HEADER:
            if (headerKeysRemaining == null) {
               headerKeysRemaining = response.getHeaderNames().iterator();
            }

            if (headerValuesRemaining == null || !headerValuesRemaining.hasNext()) {
               if (headerKeysRemaining.hasNext()) {
                  final String headerKey = headerKeysRemaining.next();
                  currentHeaderKeyBytes = ASCII_ENCODER.encode(headerKey);

                  headerValuesRemaining = response.getHeaders(headerKey).iterator();
               } else {
                  return true;
               }
            }

            if (headerValuesRemaining.hasNext()) {
               final String nextVal = headerValuesRemaining.next();

               if (nextVal != null) {
                  buffer.writeBytes(currentHeaderKeyBytes);
                  buffer.writeBytes(HEADER_SEPERATOR.getBytes());
                  buffer.writeBytes(ASCII_ENCODER.encode(nextVal));
                  buffer.writeBytes(LINE_END.getBytes());
               }
            }
            break;
      }

      return false;
   }
}
