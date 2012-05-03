package org.openrepose.rnxp.decoder;

import org.openrepose.rnxp.decoder.partial.impl.HttpErrorPartial;
import org.openrepose.rnxp.decoder.processor.HeaderProcessor;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.jboss.netty.buffer.ChannelBuffer;

import org.openrepose.rnxp.decoder.partial.impl.StatusCodePartial;
import static org.openrepose.rnxp.decoder.DecoderState.*;
import static org.openrepose.rnxp.decoder.AsciiCharacterConstant.*;

public class HttpResponseDecoder extends AbstractHttpMessageDecoder {

   private final HeaderProcessor fHeadHeaderProcessor;
   private HeaderProcessor currentHeaderProcessor;

   public class ResponseHeaderProcessor implements HeaderProcessor {

      @Override
      public HttpErrorPartial processHeader(String key, String value) {
         HttpErrorPartial messagePartial = null;

         if (key.equals("content-length") && getContentPresence() != ContentPresence.CHUNKED) {
            try {
               setContentLength(Long.parseLong(value));
               setContentPresence(ContentPresence.STATIC_LENGTH);
            } catch (NumberFormatException nfe) {
               messagePartial = HttpErrors.malformedContentLength();
            }
         } else if (key.equals("transfer-encoding") && value.equalsIgnoreCase("chunked")) {
            setContentLength(-1);
            setContentPresence(ContentPresence.CHUNKED);
         }

         return messagePartial;
      }

      @Override
      public void finishedReadingHeaders() {
         currentHeaderProcessor = null;
      }
   }

   public HttpResponseDecoder() {
      fHeadHeaderProcessor = new ResponseHeaderProcessor();

      currentHeaderProcessor = fHeadHeaderProcessor;
   }

   @Override
   protected DecoderState initialState() {
      return READ_VERSION;
   }

   @Override
   protected HttpMessagePartial httpDecode(ChannelBuffer readBuffer) {
      try {
         switch (getDecoderState()) {
            case READ_VERSION:
               return readResponseVersion(readBuffer);

            case READ_STATUS_CODE:
               return readStatusCode(readBuffer);

            case READ_REASON_PHRASE:
               if (readUntilCaseInsensitive(readBuffer, CARRIAGE_RETURN) != null) {
                  // Skip the LF
                  skipFollowingBytes(1);
                  setDecoderState(READ_HEADER_KEY);
               }
               break;

            case READ_HEADER_KEY:
               return readHeaderKey(readBuffer, currentHeaderProcessor);

            case READ_HEADER_VALUE:
               return readHeaderValue(readBuffer, currentHeaderProcessor);

            case READ_CONTENT:
               return readContent(readBuffer);

            case READ_CHUNK_LENGTH:
               return readContentChunkLength(readBuffer);

            case READ_CONTENT_CHUNKED:
               return readContentChunked(readBuffer);

            case STOP:
         }
      } catch (IndexOutOfBoundsException boundsException) {
         // TODO:Review - Log this?

         return HttpErrors.bufferOverflow(getDecoderState());
      }

      return null;
   }

   private HttpMessagePartial readResponseVersion(ChannelBuffer socketBuffer) {
      HttpMessagePartial messagePartial = readHttpVersion(socketBuffer, SPACE);

      if (messagePartial != null) {
         setDecoderState(READ_STATUS_CODE);
      }

      return messagePartial;
   }

   private HttpMessagePartial readStatusCode(ChannelBuffer socketBuffer) {
      HttpMessagePartial messagePartial = null;

      if (readUntilCaseInsensitive(socketBuffer, SPACE) != null) {
         final String versionString = flushCharacterBuffer();
         HttpStatusCode code;

         try {
            // TODO:Review - Is this a good idea? Should we just past the code as is and adhere to validation rules in the RFC?
            code = HttpStatusCode.fromInt(Integer.parseInt(versionString));

            if (code != HttpStatusCode.UNSUPPORTED_RESPONSE_CODE) {
               messagePartial = new StatusCodePartial(code);
               setDecoderState(READ_REASON_PHRASE);
            } else {
               messagePartial = HttpErrors.badStatusCode("Unknown status code");
            }
         } catch (NumberFormatException exception) {
            return HttpErrors.badStatusCode("Bad status code format");
         }
      }

      return messagePartial;
   }
}