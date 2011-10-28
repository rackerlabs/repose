package org.openrepose.rnxp.decoder;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import org.junit.Test;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.util.CharsetUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.openrepose.rnxp.decoder.partial.ContentMessagePartial;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.HeaderPartial;
import org.openrepose.rnxp.decoder.partial.impl.HttpErrorPartial;
import org.openrepose.rnxp.decoder.partial.impl.HttpVersionPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestMethodPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestUriPartial;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.HttpMethod;
import static org.junit.Assert.*;
import static org.jboss.netty.buffer.ChannelBuffers.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class HttpRequestDecoderTest {

    @Ignore
    public static class HttpDecoderTestPart {

        protected HttpRequestDecoder decoder;

        @Before
        public void standUp() throws Exception {
            decoder = new HttpRequestDecoder();
        }

        public HttpMessagePartial nextPartial(ChannelBuffer buffer, int expectedReads) throws Exception {
            for (int read = 1; read <= expectedReads; read++) {
                final HttpMessagePartial partial = (HttpMessagePartial) decoder.decode(null, null, buffer);

                if (partial != null) {
                    assertEquals("Decoder must decode partial in " + expectedReads + " read passes", expectedReads, read);

                    return partial;
                }
            }

            throw new HttpDecoderTestException("Reads for partial exceeded expected");
        }

        public void stepDecoderTo(DecoderState state) {
            decoder.updateState(state);
        }
    }

    public static class WhenDecodingRequestMethods extends HttpDecoderTestPart {

        private void validateDecodedMethod(HttpMethod method, RequestMethodPartial decodedPartial) {
            assertNotNull("Decoder must decode " + method.name() + " methods", decodedPartial);
            assertEquals("Decoder must decode " + method.name() + " methods after reading one byte", HttpMessageComponent.REQUEST_METHOD, decodedPartial.getHttpMessageComponent());
            assertEquals("Decoder must decode " + method.name() + " methods after reading one byte", method, decodedPartial.getHttpMethod());
        }

        @Test
        public void shouldDecodeGET() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("G".getBytes(CharsetUtil.US_ASCII));

            final RequestMethodPartial decodedPartial = (RequestMethodPartial) nextPartial(buffer, 1);
            validateDecodedMethod(HttpMethod.GET, decodedPartial);
        }

        @Test
        public void shouldDecodeDELETE() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("D".getBytes(CharsetUtil.US_ASCII));

            final RequestMethodPartial decodedPartial = (RequestMethodPartial) nextPartial(buffer, 1);
            validateDecodedMethod(HttpMethod.DELETE, decodedPartial);
        }

        @Test
        public void shouldDecodeHEAD() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("H".getBytes(CharsetUtil.US_ASCII));

            final RequestMethodPartial decodedPartial = (RequestMethodPartial) nextPartial(buffer, 1);
            validateDecodedMethod(HttpMethod.HEAD, decodedPartial);
        }

        @Test
        public void shouldDecodeOPTIONS() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("O".getBytes(CharsetUtil.US_ASCII));

            final RequestMethodPartial decodedPartial = (RequestMethodPartial) nextPartial(buffer, 1);
            validateDecodedMethod(HttpMethod.OPTIONS, decodedPartial);
        }

        @Test
        public void shouldDecodeTRACE() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("T".getBytes(CharsetUtil.US_ASCII));

            final RequestMethodPartial decodedPartial = (RequestMethodPartial) nextPartial(buffer, 1);
            validateDecodedMethod(HttpMethod.TRACE, decodedPartial);
        }

        @Test
        public void shouldDecodePUT() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("PU".getBytes(CharsetUtil.US_ASCII));

            final RequestMethodPartial decodedPartial = (RequestMethodPartial) nextPartial(buffer, 2);
            validateDecodedMethod(HttpMethod.PUT, decodedPartial);
        }

        @Test
        public void shouldDecodePOST() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("PO".getBytes(CharsetUtil.US_ASCII));

            final RequestMethodPartial decodedPartial = (RequestMethodPartial) nextPartial(buffer, 2);
            validateDecodedMethod(HttpMethod.POST, decodedPartial);
        }
    }

    public static class WhenDecodingRequestUri extends HttpDecoderTestPart {

        @Override
        public void standUp() throws Exception {
            super.standUp();

            stepDecoderTo(DecoderState.READ_URI);
        }

        @Test
        public void shouldDecodeValidUri() throws Exception {
            final String expectedUri = "/path", requestedUri = expectedUri + " ";
            final ChannelBuffer buffer = copiedBuffer(requestedUri.getBytes(CharsetUtil.US_ASCII));

            final RequestUriPartial uriPartial = (RequestUriPartial) nextPartial(buffer, 6);

            assertEquals("Decoder must decode valid URIs", expectedUri, uriPartial.getRequestUri());
        }
    }

    public static class WhenDecodingRequestHttpVersion extends HttpDecoderTestPart {

        @Override
        public void standUp() throws Exception {
            super.standUp();

            stepDecoderTo(DecoderState.READ_VERSION);
        }

        @Test
        public void shouldFailInvalidHttpVersions() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("XTTP/1.1\r\n".getBytes(CharsetUtil.US_ASCII));

            final HttpErrorPartial versionPartial = (HttpErrorPartial) nextPartial(buffer, 1);

            assertEquals("Decoder must decode valid request HTTP versions", HttpStatusCode.BAD_REQUEST, versionPartial.getStatusCode());
        }

        @Test
        public void shouldDecodeValidHttpVersions() throws Exception {
            final String expectedVersion = "1.1";
            final ChannelBuffer buffer = copiedBuffer("HTTP/1.1\r\n".getBytes(CharsetUtil.US_ASCII));

            final HttpVersionPartial versionPartial = (HttpVersionPartial) nextPartial(buffer, 9);

            assertEquals("Decoder must decode valid request HTTP versions", expectedVersion, versionPartial.getHttpVersion());
        }
    }

    public static class WhenDecodingRequestHeader extends HttpDecoderTestPart {

        @Override
        public void standUp() throws Exception {
            super.standUp();

            stepDecoderTo(DecoderState.READ_HEADER_KEY);
        }

        @Test
        public void shouldRejectEmptyHeaderKeys() throws Exception {
            final String expectedHeaderKey = "";
            final String expectedHeaderValue = "tHiSiSaHoSt.CoM";
            final String completeHeaderLine = expectedHeaderKey.toUpperCase() + ":" + expectedHeaderValue + "\r\n";

            final ChannelBuffer buffer = copiedBuffer(completeHeaderLine.getBytes(CharsetUtil.US_ASCII));

            final HttpErrorPartial errorPartial = (HttpErrorPartial) nextPartial(buffer, 1);

            assertNotNull("Decoder must identify empty header keys", errorPartial);
            assertEquals("Decoder must communicate 400 for empty header keys", HttpStatusCode.BAD_REQUEST, errorPartial.getStatusCode());
        }

        @Test
        public void shouldRejectBadContentLength() throws Exception {
            final String expectedHeaderKey = "content-length";
            final String expectedHeaderValue = "nan";
            final String completeHeaderLine = expectedHeaderKey.toUpperCase() + ":" + expectedHeaderValue + "\r\n";

            final ChannelBuffer buffer = copiedBuffer(completeHeaderLine.getBytes(CharsetUtil.US_ASCII));

            final HttpErrorPartial errorPartial = (HttpErrorPartial) nextPartial(buffer, 19);

            assertNotNull("Decoder must identify empty header keys", errorPartial);
            assertEquals("Decoder must communicate 400 for empty header keys", HttpStatusCode.BAD_REQUEST, errorPartial.getStatusCode());
        }

        @Test
        public void shouldDecodeValidHeader() throws Exception {
            final String expectedHeaderKey = "host";
            final String expectedHeaderValue = "tHiSiSaHoSt.CoM";
            final String completeHeaderLine = expectedHeaderKey.toUpperCase() + ":" + expectedHeaderValue + "\r\n";

            final ChannelBuffer buffer = copiedBuffer(completeHeaderLine.getBytes(CharsetUtil.US_ASCII));

            final HeaderPartial headerPartial = (HeaderPartial) nextPartial(buffer, 21);

            assertEquals("Decoder must decode valid request HTTP header keys", expectedHeaderKey, headerPartial.getHeaderKey());
            assertEquals("Decoder must decode valid request HTTP header values", expectedHeaderValue, headerPartial.getHeaderValue());
        }
    }

    public static class WhenDecodingRequestContent extends HttpDecoderTestPart {

        @Override
        public void standUp() throws Exception {
            super.standUp();

            stepDecoderTo(DecoderState.READ_HEADER_KEY);
        }

        @Test
        public void shouldDecodeStaticLengthContent() throws Exception {
            final String expectedHeaderKey = "content-length";
            final String expectedHeaderValue = "15";
            final String completeHeaderLine = expectedHeaderKey + ":" + expectedHeaderValue;

            final String content = "This is conent!";
            final byte[] expectedContent = content.getBytes(CharsetUtil.US_ASCII);

            final String fullRequestFragment = completeHeaderLine + "\r\n\r\n" + content;
            final ChannelBuffer buffer = copiedBuffer(fullRequestFragment.getBytes(CharsetUtil.US_ASCII));

            // Read the header
            nextPartial(buffer, 18);
            
            assertEquals("Next partial must be CONTENT_START", nextPartial(buffer, 2).getHttpMessageComponent(), HttpMessageComponent.CONTENT_START);

            for (int i = 0; i < expectedContent.length - 1;) {
                final HttpMessagePartial rawHttpMessagePartial = (HttpMessagePartial) decoder.decode(null, null, buffer);

                if (rawHttpMessagePartial != null) {
                    assertEquals("Content must be tranmitted in ContentMessagePartials", ContentMessagePartial.class, rawHttpMessagePartial.getClass());

                    final ContentMessagePartial contentPartial = (ContentMessagePartial) rawHttpMessagePartial;
                    assertEquals("Decoder provided content that does not match expected", expectedContent[i++], contentPartial.getData());
                }
            }

            final ContentMessagePartial contentPartial = (ContentMessagePartial) decoder.decode(null, null, buffer);
            assertEquals("Decoder must end static length content", HttpMessageComponent.MESSAGE_END_WITH_CONTENT, contentPartial.getHttpMessageComponent());
        }

        @Test
        public void shouldDecodeChunkedContent() throws Exception {
            final String transferEncoding = "transfer-encoding:chunked";

            final String actualContent = "12345678123456781234567812345678";
            final byte[] expectedContent = actualContent.getBytes(CharsetUtil.US_ASCII);

            final String chunkedContent = "8\r\n12345678\r\n8\r\n12345678\r\n10\r\n1234567812345678\r\n0\r\n\r\n";
            
            final String fullRequestFragment = transferEncoding + "\r\n\r\n" + chunkedContent;
            final ChannelBuffer buffer = copiedBuffer(fullRequestFragment.getBytes(CharsetUtil.US_ASCII));

            // Read the headers
            nextPartial(buffer, 26);
            
            assertEquals("Next partial must be CONTENT_START", HttpMessageComponent.CONTENT_START, nextPartial(buffer, 2).getHttpMessageComponent());

            for (int i = 0; i < expectedContent.length;) {
                final HttpMessagePartial rawHttpMessagePartial = (HttpMessagePartial) decoder.decode(null, null, buffer);

                if (rawHttpMessagePartial != null) {
                    assertEquals("Content must be tranmitted in ContentMessagePartials on index " + i, ContentMessagePartial.class, rawHttpMessagePartial.getClass());

                    final ContentMessagePartial contentPartial = (ContentMessagePartial) rawHttpMessagePartial;
                    assertEquals("Decoder provided content that does not match expected on index " + i, Character.valueOf((char) expectedContent[i++]), Character.valueOf((char) contentPartial.getData()));
                }
            }

            assertEquals("Decoder must end chunked content", HttpMessageComponent.MESSAGE_END_NO_CONTENT, nextPartial(buffer, 6).getHttpMessageComponent());
        }

        @Test
        public void shouldDecodeChunkedContentWithContentLengthSpecifiedFirst() throws Exception {
            final String contentLength = "content-length:32";
            final String transferEncoding = "transfer-encoding:chunked";

            final String actualContent = "12345678123456781234567812345678";
            final byte[] expectedContent = actualContent.getBytes(CharsetUtil.US_ASCII);

            final String chunkedContent = "8\r\n12345678\r\n8\r\n12345678\r\n10\r\n1234567812345678\r\n0\r\n\r\n";
            
            final String fullRequestFragment = contentLength + "\r\n" + transferEncoding + "\r\n\r\n" + chunkedContent;
            final ChannelBuffer buffer = copiedBuffer(fullRequestFragment.getBytes(CharsetUtil.US_ASCII));

            // Read the headers
            nextPartial(buffer, 18);
            nextPartial(buffer, 27);
            
            assertEquals("Next partial must be CONTENT_START", nextPartial(buffer, 2).getHttpMessageComponent(), HttpMessageComponent.CONTENT_START);

            for (int i = 0; i < expectedContent.length;) {
                final HttpMessagePartial rawHttpMessagePartial = (HttpMessagePartial) decoder.decode(null, null, buffer);

                if (rawHttpMessagePartial != null) {
                    assertEquals("Content must be tranmitted in ContentMessagePartials on index " + i, ContentMessagePartial.class, rawHttpMessagePartial.getClass());

                    final ContentMessagePartial contentPartial = (ContentMessagePartial) rawHttpMessagePartial;
                    assertEquals("Decoder provided content that does not match expected on index " + i, Character.valueOf((char) expectedContent[i++]), Character.valueOf((char) contentPartial.getData()));
                }
            }

            assertEquals("Decoder must end chunked content", HttpMessageComponent.MESSAGE_END_NO_CONTENT, nextPartial(buffer, 6).getHttpMessageComponent());
        }

        @Test
        public void shouldDecodeChunkedContentWithContentLengthSpecifiedLast() throws Exception {
            final String contentLength = "content-length:32";
            final String transferEncoding = "transfer-encoding:chunked";

            final String actualContent = "12345678123456781234567812345678";
            final byte[] expectedContent = actualContent.getBytes(CharsetUtil.US_ASCII);

            final String chunkedContent = "8\r\n12345678\r\n8\r\n12345678\r\n10\r\n1234567812345678\r\n0\r\n\r\n";
            
            final String fullRequestFragment = transferEncoding + "\r\n" + contentLength + "\r\n\r\n" + chunkedContent;
            final ChannelBuffer buffer = copiedBuffer(fullRequestFragment.getBytes(CharsetUtil.US_ASCII));

            // Read the headers
            nextPartial(buffer, 26);
            nextPartial(buffer, 19);
            
            assertEquals("Next partial must be CONTENT_START", nextPartial(buffer, 2).getHttpMessageComponent(), HttpMessageComponent.CONTENT_START);

            for (int i = 0; i < expectedContent.length;) {
                final HttpMessagePartial rawHttpMessagePartial = (HttpMessagePartial) decoder.decode(null, null, buffer);

                if (rawHttpMessagePartial != null) {
                    assertEquals("Content must be tranmitted in ContentMessagePartials on index " + i, ContentMessagePartial.class, rawHttpMessagePartial.getClass());

                    final ContentMessagePartial contentPartial = (ContentMessagePartial) rawHttpMessagePartial;
                    assertEquals("Decoder provided content that does not match expected on index " + i, Character.valueOf((char) expectedContent[i++]), Character.valueOf((char) contentPartial.getData()));
                }
            }

            assertEquals("Decoder must end chunked content", HttpMessageComponent.MESSAGE_END_NO_CONTENT, nextPartial(buffer, 6).getHttpMessageComponent());
        }
    }
}
