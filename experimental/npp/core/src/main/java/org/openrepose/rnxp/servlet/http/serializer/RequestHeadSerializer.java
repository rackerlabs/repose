package org.openrepose.rnxp.servlet.http.serializer;

import com.rackspace.papi.commons.util.StringUtilities;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import org.jboss.netty.buffer.ChannelBuffer;
import org.openrepose.rnxp.http.HttpMessageComponent;
import static org.openrepose.rnxp.http.io.control.HttpControlSequence.*;

/**
 *
 * @author zinic
 */
public class RequestHeadSerializer extends AbstractHttpMessageSerializer {
   
   private final HttpServletRequest request;
   private final Enumeration<String> headerKeys;
   
   private byte[] currentHeaderKeyBytes;
   private Enumeration<String> headerValues;
   private HttpMessageComponent nextComponent;

   public RequestHeadSerializer(HttpServletRequest request) {
      this.request = request;
      
      headerKeys = request.getHeaderNames();
      nextComponent = HttpMessageComponent.REQUEST_METHOD;
   }

   @Override
   public void serializeNext() {
      final ChannelBuffer buffer = getBuffer();
      
      switch (nextComponent) {
         case REQUEST_METHOD:
            buffer.writeBytes(ASCII_ENCODER.encode(request.getMethod()));
            buffer.writeBytes(SPACE.getBytes());
            
            nextComponent = HttpMessageComponent.REQUEST_URI;
            break;
            
         case REQUEST_URI:
            buffer.writeBytes(ASCII_ENCODER.encode(request.getRequestURI()));
            
            if (StringUtilities.isNotBlank(request.getQueryString())) {
               buffer.writeBytes(QUERY_PARAMETER_SEPERATOR.getBytes());
               buffer.writeBytes(ASCII_ENCODER.encode(request.getQueryString()));
            }
            
            nextComponent = HttpMessageComponent.HTTP_VERSION;
            break;
            
         case HTTP_VERSION:
            buffer.writeBytes(HTTP_VERSION.getBytes());
            buffer.writeBytes(SPACE.getBytes());

            nextComponent = HttpMessageComponent.HEADER;
            break;

         case HEADER:
            if (headerValues == null || !headerValues.hasMoreElements()) {
               if (headerKeys.hasMoreElements()) {
                  final String headerKey = headerKeys.nextElement();
                  currentHeaderKeyBytes = ASCII_ENCODER.encode(headerKey);

                  headerValues = request.getHeaders(headerKey);
               } else {
                  serializationFinished();
               }
            } else {
               final String nextVal = headerValues.nextElement();

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
