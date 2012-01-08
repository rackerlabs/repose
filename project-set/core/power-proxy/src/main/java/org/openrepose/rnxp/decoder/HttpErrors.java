package org.openrepose.rnxp.decoder;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import org.openrepose.rnxp.decoder.partial.impl.HttpErrorPartial;
import org.openrepose.rnxp.http.HttpMessageComponent;

/**
 * Reusable HTTP Errors
 * 
 * @author zinic
 */
public class HttpErrors {

    // Error Constants
    private static final HttpErrorPartial METHOD_NOT_IMPLEMENTED = new HttpErrorPartial(HttpMessageComponent.REQUEST_METHOD, HttpStatusCode.NOT_IMPLEMENTED, "Method not supported");
    private static final HttpErrorPartial HTTP_VERSION_BAD = new HttpErrorPartial(HttpMessageComponent.HEADER, HttpStatusCode.BAD_REQUEST, "HTTP version malformed");
    private static final HttpErrorPartial HTTP_VERSION_UNSUPPORTED = new HttpErrorPartial(HttpMessageComponent.HEADER, HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED, "HTTP version unsupported");
    private static final HttpErrorPartial BAD_HEADER_KEY = new HttpErrorPartial(HttpMessageComponent.HEADER, HttpStatusCode.BAD_REQUEST, "Headers keys must have at least one valid character");
    private static final HttpErrorPartial MALFORMED_CONTENT_LENGTH = new HttpErrorPartial(HttpMessageComponent.HEADER, HttpStatusCode.BAD_REQUEST, "Content length must be a valid, positive long");
    private static final HttpErrorPartial MALFORMED_CHUNK_LENGTH = new HttpErrorPartial(HttpMessageComponent.HEADER, HttpStatusCode.BAD_REQUEST, "Chunk length must be a valid, hex coded positive long");
    
    
    // String Constants
    private static final String BUFFER_OVERFLOW_MESSAGE = "Your message has an element that is too large to process.";

    public static HttpErrorPartial bufferOverflow(DecoderState decoderState) {
        switch (decoderState) {
            case READ_SC_PARSE_METHOD:
            case READ_MC_PARSE_METHOD:
                return new HttpErrorPartial(HttpMessageComponent.REQUEST_METHOD, HttpStatusCode.BAD_REQUEST, BUFFER_OVERFLOW_MESSAGE);

            case READ_URI:
                return new HttpErrorPartial(HttpMessageComponent.REQUEST_URI, HttpStatusCode.BAD_REQUEST, BUFFER_OVERFLOW_MESSAGE);

            case READ_VERSION:
                return new HttpErrorPartial(HttpMessageComponent.REQUEST_METHOD, HttpStatusCode.BAD_REQUEST, BUFFER_OVERFLOW_MESSAGE);

            case READ_HEADER_KEY:
            case READ_HEADER_VALUE:
                return new HttpErrorPartial(HttpMessageComponent.HEADER, HttpStatusCode.BAD_REQUEST, BUFFER_OVERFLOW_MESSAGE);

            case STOP:
                return new HttpErrorPartial(HttpMessageComponent.MESSAGE_END_NO_CONTENT, HttpStatusCode.BAD_REQUEST, BUFFER_OVERFLOW_MESSAGE);

            default:
                return new HttpErrorPartial(null, HttpStatusCode.BAD_REQUEST, BUFFER_OVERFLOW_MESSAGE);
        }
    }
    
    public static HttpErrorPartial badStatusCode(String msg) {
       return new HttpErrorPartial(HttpMessageComponent.HEADER, HttpStatusCode.BAD_REQUEST, msg);
    }
    
    public static HttpErrorPartial malformedContentLength() {
        return MALFORMED_CONTENT_LENGTH;
    }
    
    public static HttpErrorPartial malformedChunkLength() {
        return MALFORMED_CHUNK_LENGTH;
    }

    public static HttpErrorPartial badVersion() {
        return HTTP_VERSION_BAD;
    }

    public static HttpErrorPartial unsupportedVersion() {
        return HTTP_VERSION_UNSUPPORTED;
    }

    public static HttpErrorPartial methodNotImplemented() {
        return METHOD_NOT_IMPLEMENTED;
    }

    public static HttpErrorPartial badHeaderKey() {
        return BAD_HEADER_KEY;
    }
}
