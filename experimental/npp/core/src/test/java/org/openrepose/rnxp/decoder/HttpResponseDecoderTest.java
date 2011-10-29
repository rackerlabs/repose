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
import org.openrepose.rnxp.decoder.partial.impl.StatusCodePartial;
import org.openrepose.rnxp.http.HttpMessageComponent;
import static org.junit.Assert.*;
import static org.jboss.netty.buffer.ChannelBuffers.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class HttpResponseDecoderTest {

    @Ignore
    public static class HttpDecoderTestPart {

        protected HttpResponseDecoder decoder;

        @Before
        public void standUp() throws Exception {
            decoder = new HttpResponseDecoder();
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
            decoder.setDecoderState(state);
        }
    }

    public static class WhenDecodingRequestHttpVersion extends HttpDecoderTestPart {

        @Override
        public void standUp() throws Exception {
            super.standUp();

            stepDecoderTo(DecoderState.READ_VERSION);
        }

        @Test
        public void shouldDecodeValidHttpVersions() throws Exception {
            final String expectedVersion = "1.1";
            final ChannelBuffer buffer = copiedBuffer("HTTP/1.1\r\n".getBytes(CharsetUtil.US_ASCII));

            final HttpVersionPartial versionPartial = (HttpVersionPartial) nextPartial(buffer, 9);

            assertEquals("Decoder must decode valid request HTTP versions", expectedVersion, versionPartial.getHttpVersion());
        }
    }

    public static class WhenDecodingResponseStatusCodes extends HttpDecoderTestPart {

        @Override
        public void standUp() throws Exception {
            super.standUp();

            stepDecoderTo(DecoderState.READ_STATUS_CODE);
        }

        @Test
        public void shouldDecodeValidHttpVersions() throws Exception {
            final HttpStatusCode expectedCode = HttpStatusCode.OK;
            final ChannelBuffer buffer = copiedBuffer((expectedCode.intValue() + " ").getBytes(CharsetUtil.US_ASCII));

            final StatusCodePartial statusCodePartial = (StatusCodePartial) nextPartial(buffer, 4);

            assertEquals("Decoder must decode valid request HTTP versions", expectedCode, statusCodePartial.getStatusCode());
        }
    }

    @Ignore
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

    @Ignore
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

        private void readAndValidateContent(final byte[] expectedContent, final ChannelBuffer buffer) throws Exception {
            for (int i = 0; i < expectedContent.length;) {
                final HttpMessagePartial rawHttpMessagePartial = (HttpMessagePartial) decoder.decode(null, null, buffer);

                if (rawHttpMessagePartial != null) {
                    assertEquals("Content must be tranmitted in ContentMessagePartials on index " + i, ContentMessagePartial.class, rawHttpMessagePartial.getClass());

                    final ContentMessagePartial contentPartial = (ContentMessagePartial) rawHttpMessagePartial;
                    assertEquals("Decoder provided content that does not match expected on index " + i, Character.valueOf((char) expectedContent[i++]), Character.valueOf((char) contentPartial.getData()));
                }
            }
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
            readAndValidateContent(expectedContent, buffer);

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
            readAndValidateContent(expectedContent, buffer);

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
            readAndValidateContent(expectedContent, buffer);

            assertEquals("Decoder must end chunked content", HttpMessageComponent.MESSAGE_END_NO_CONTENT, nextPartial(buffer, 6).getHttpMessageComponent());
        }
    }
}
