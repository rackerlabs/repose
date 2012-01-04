package org.openrepose.rnxp.servlet.http.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import javax.servlet.http.HttpServletResponse;
import org.jboss.netty.buffer.ChannelBuffer;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.util.StringCharsetEncoder;

import static org.jboss.netty.buffer.ChannelBuffers.*;
import static org.openrepose.rnxp.http.io.control.HttpControlSequence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class ResponseHeadSerializer extends AbstractHttpMessageSerializer {

   private final HttpServletResponse response;
   private final Iterator<String> headerKeys;
   private byte[] currentHeaderKeyBytes;
   private Iterator<String> headerValues;
   private HttpMessageComponent nextComponent;

   public ResponseHeadSerializer(HttpServletResponse response) {
      this.response = response;

      headerKeys = response.getHeaderNames().iterator();
      nextComponent = HttpMessageComponent.HTTP_VERSION;
   }

   @Override
   public void serializeNext() {
      final ChannelBuffer buffer = getBuffer();
      
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
            if (headerValues == null || !headerValues.hasNext()) {
               if (headerKeys.hasNext()) {
                  final String headerKey = headerKeys.next();
                  currentHeaderKeyBytes = ASCII_ENCODER.encode(headerKey);

                  headerValues = response.getHeaders(headerKey).iterator();
               } else {
                  serializationFinished();
               }
            } else {
               final String nextVal = headerValues.next();

               if (nextVal != null) {
                  buffer.writeBytes(currentHeaderKeyBytes);
                  buffer.writeBytes(HEADER_SEPERATOR.getBytes());
                  buffer.writeBytes(ASCII_ENCODER.encode(nextVal));
                  buffer.writeBytes(LINE_END.getBytes());
               }
            }
            break;
      }
   }
}
