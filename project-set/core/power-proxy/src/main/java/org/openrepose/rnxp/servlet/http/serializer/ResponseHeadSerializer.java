package org.openrepose.rnxp.servlet.http.serializer;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import java.util.Iterator;
import javax.servlet.http.HttpServletResponse;
import org.jboss.netty.buffer.ChannelBuffer;
import org.openrepose.rnxp.http.HttpMessageComponent;
import static org.openrepose.rnxp.http.io.control.HttpControlSequence.*;

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
            final HttpStatusCode code = HttpStatusCode.fromInt(response.getStatus());
            
            buffer.writeBytes(ASCII_ENCODER.encode(code.intValue()));
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
                  buffer.writeBytes(LINE_END.getBytes());
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
