package org.openrepose.rnxp.decoder;

import org.openrepose.rnxp.http.domain.HttpPartial;
import org.junit.Test;
import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.openrepose.rnxp.http.domain.HttpMessageComponent;
import org.openrepose.rnxp.http.domain.HttpMethod;
import static org.junit.Assert.*;
import static org.jboss.netty.buffer.ChannelBuffers.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class HttpDecoderTest {

    @Ignore
    public static class HttpDecoderTestPart {

        protected HttpDecoder decoder;

        @Before
        public void standUp() throws Exception {
            decoder = new HttpDecoder();
        }

        public HttpPartial nextPartial(ChannelBuffer buffer, int expectedReads) throws Exception {
            for (int read = 0; read < expectedReads; read++) {
                final HttpPartial partial = (HttpPartial) decoder.decode(null, null, buffer);

                if (partial != null) {
                    assertEquals("Decoder must decode partial in " + expectedReads + " read passes", expectedReads - 1, read);

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

        private void validateDecodedMethod(HttpMethod method, HttpPartial decodedPartial) {
            assertNotNull("Decoder must decode " + method.name() + " methods", decodedPartial);
            assertEquals("Decoder must decode " + method.name() + " methods after reading one byte", HttpMessageComponent.REQUEST_METHOD, decodedPartial.messageComponent());
            assertEquals("Decoder must decode " + method.name() + " methods after reading one byte", method, decodedPartial.getMethod());
        }

        @Test
        public void shouldDecodeGET() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("G".getBytes());

            final HttpPartial decodedPartial = nextPartial(buffer, 1);
            validateDecodedMethod(HttpMethod.GET, decodedPartial);
        }

        @Test
        public void shouldDecodeDELETE() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("D".getBytes());

            final HttpPartial decodedPartial = nextPartial(buffer, 1);
            validateDecodedMethod(HttpMethod.DELETE, decodedPartial);
        }

        @Test
        public void shouldDecodeHEAD() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("H".getBytes());

            final HttpPartial decodedPartial = nextPartial(buffer, 1);
            validateDecodedMethod(HttpMethod.HEAD, decodedPartial);
        }

        @Test
        public void shouldDecodeOPTIONS() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("O".getBytes());

            final HttpPartial decodedPartial = nextPartial(buffer, 1);
            validateDecodedMethod(HttpMethod.OPTIONS, decodedPartial);
        }

        @Test
        public void shouldDecodeTRACE() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("T".getBytes());

            final HttpPartial decodedPartial = nextPartial(buffer, 1);
            validateDecodedMethod(HttpMethod.TRACE, decodedPartial);
        }

        @Test
        public void shouldDecodePUT() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("PU".getBytes());

            final HttpPartial decodedPartial = nextPartial(buffer, 2);
            validateDecodedMethod(HttpMethod.PUT, decodedPartial);
        }

        @Test
        public void shouldDecodePOST() throws Exception {
            final ChannelBuffer buffer = copiedBuffer("PO".getBytes());

            final HttpPartial decodedPartial = nextPartial(buffer, 2);
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
            final ChannelBuffer buffer = copiedBuffer(requestedUri.getBytes());

            final HttpPartial uriPartial = nextPartial(buffer, 1);

            assertEquals("Decoder must decode valid URIs", expectedUri, uriPartial.getPartial());
        }
    }

    public static class WhenDecodingRequestHttpVersion extends HttpDecoderTestPart {

        @Override
        public void standUp() throws Exception {
            super.standUp();

            stepDecoderTo(DecoderState.READ_VERSION);
        }

        @Test
        public void shouldDecodeValidUri() throws Exception {
            final String expectedVersion = "1.1";
            final ChannelBuffer buffer = copiedBuffer("HTTP/1.1\r\n".getBytes());

            final HttpPartial versionPartial = nextPartial(buffer, 6);

            assertEquals("Decoder must decode valid request HTTP versions", expectedVersion, versionPartial.getPartial());
        }
    }
    
    public static class WhenDecodingRequestHeader extends HttpDecoderTestPart {

        @Override
        public void standUp() throws Exception {
            super.standUp();

            stepDecoderTo(DecoderState.READ_HEADER_KEY);
        }

        @Test
        public void shouldDecodeValidHeader() throws Exception {
            final String expectedHeaderKey = "host";
            final String expectedHeaderValue = "tHiSiSaHoSt.CoM";
            final ChannelBuffer buffer = copiedBuffer((expectedHeaderKey.toUpperCase() + ":" + expectedHeaderValue + "\r\n").getBytes());

            final HttpPartial headerPartial = nextPartial(buffer, 2);

            assertEquals("Decoder must decode valid request HTTP header keys", expectedHeaderKey, headerPartial.getHeaderKey());
            assertEquals("Decoder must decode valid request HTTP header values", expectedHeaderValue, headerPartial.getHeaderValue());
        }
    }
}
