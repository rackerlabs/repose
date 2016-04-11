/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.logging.apache;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.logging.apache.format.LogArgumentFormatter;
import org.openrepose.commons.utils.logging.apache.format.stock.*;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.servlet.http.ResponseMode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class HttpLogFormatterTest {

    public static class WhenParsingSimpleArguments {

        public HttpServletResponse response;
        public HttpServletRequest request;

        @Before
        public void setup() {
            request = mock(HttpServletRequest.class);
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.place.net/u/r/l"));
            response = mock(HttpServletResponse.class);
        }

        @Test
        public void shouldCorrectlyDetectEscapeSequences() {
            final HttpLogFormatter formatter = new HttpLogFormatter("%h %% %u %U");

            assertTrue("Should have parsed seven handlers. Only found: " + formatter.getHandlerList().size(), formatter.getHandlerList().size() == 7);
        }

        @Test
        public void shouldCorrectlyParseEmptySpace() {
            final HttpLogFormatter formatter = new HttpLogFormatter("%h%%%u%U");

            assertTrue("Should have parsed four handlers. Only found: " + formatter.getHandlerList().size(), formatter.getHandlerList().size() == 4);
        }

        @Test
        public void shouldPreserveStringFormatting() {
            final HttpLogFormatter formatter = new HttpLogFormatter("%%log output%% %U");
            final String expected = "%log output% http://some.place.net/u/r/l";

            assertEquals(expected, formatter.format(request, response));
        }

        @Test
        public void shouldCorrectlyConstructRequestLine() {
            when(request.getProtocol()).thenReturn("HTTP/1.1");
            when(request.getRequestURI()).thenReturn("/index.html");
            when(request.getMethod()).thenReturn("GET");

            final HttpLogFormatter formatter = new HttpLogFormatter("%r");
            final String expected = "GET /index.html HTTP/1.1";

            assertEquals(expected, formatter.format(request, response));
        }

        @Test
        public void shouldReplaceTokenWithRequestGuid() {
            final HttpLogFormatter formatter = new HttpLogFormatter("%" + LogFormatArgument.TRACE_GUID.toString());
            final String expected = "test-guid";

            Vector<String> reqGuidValues = new Vector<>();
            reqGuidValues.add("test-guid");

            when(request.getHeaders(CommonHttpHeader.TRACE_GUID.toString()))
                    .thenReturn(reqGuidValues.elements());

            assertEquals(expected, formatter.format(request, response));
        }

        @Test
        public void shouldParseSimpleTimeFormat() {
            final String defaultDateFormatRegex = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

            final HttpLogFormatter formatter = new HttpLogFormatter("%t");

            assertEquals(1, formatter.getHandlerList().size());
            assertTrue(Pattern.matches(defaultDateFormatRegex, formatter.format(request, response)));
        }
    }

    public static class WhenParsingComplexArguments {

        public HttpServletResponse response;
        public HttpServletRequest request;

        @Before
        public void setup() {
            request = mock(HttpServletRequest.class);
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.place.net/u/r/l"));
            response = mock(HttpServletResponse.class);
        }

        @Test
        public void shouldParseInclusiveStatusCodeRestrictions() {
            final HttpLogFormatter formatter = new HttpLogFormatter("%200,201U");
            final String expected = "http://some.place.net/u/r/l";

            when(response.getStatus()).thenReturn(200);
            assertEquals(expected, formatter.format(request, response));
            when(response.getStatus()).thenReturn(401);
            assertEquals("-", formatter.format(request, response));
        }

        @Test
        public void shouldParseExclusiveStatusCodeRestrictions() {
            final HttpLogFormatter formatter = new HttpLogFormatter("%!401,403U");
            final String expected = "http://some.place.net/u/r/l";

            assertEquals(expected, formatter.format(request, response));
            when(response.getStatus()).thenReturn(401);
            assertEquals("-", formatter.format(request, response));
        }

        @Test
        public void shouldParseCustomTimeFormat() {
            final String customDateFormatRegex = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

            final HttpLogFormatter httpLogFormatter = new HttpLogFormatter("%{yyyy-MM-dd HH:mm:ss}t");

            assertEquals(1, httpLogFormatter.getHandlerList().size());
            assertTrue(Pattern.matches(customDateFormatRegex, httpLogFormatter.format(request, response)));
        }
    }

    public static class WhenSettingLogic {

        private LogArgumentFormatter formatter;
        private HttpLogFormatter httpLogFormatter;

        @Before
        public void setup() {
            formatter = new LogArgumentFormatter();
            httpLogFormatter = new HttpLogFormatter("%h %% %u %U");
        }

        @Test
        public void shouldNotHaveSetLogicByDefault() {
            assertNull(formatter.getLogic());
        }


        @Test
        public void ResponseTimeHandlerMicroseconds() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.RESPONSE_TIME_MICROSECONDS.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof ResponseTimeHandler);
        }

        @Test
        public void ResponseTimeHandlerSeconds() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.RESPONSE_TIME_SECONDS.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof ResponseTimeHandler);
        }

        @Test
        public void RequestHeaderHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.REQUEST_HEADER.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof RequestHeaderHandler);
        }

        @Test
        public void RequestLineHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.REQUEST_LINE.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof RequestLineHandler);
        }

        @Test
        public void RequestProtocolHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.REQUEST_PROTOCOL.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof RequestProtocolHandler);
        }

        @Test
        public void ResponseHeaderHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.RESPONSE_HEADER.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof ResponseHeaderHandler);
        }

        @Test
        public void CanonicalPortHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.CANONICAL_PORT.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof CanonicalPortHandler);
        }

        @Test
        public void LocalAddressHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.LOCAL_ADDRESS.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof LocalAddressHandler);
        }

        @Test
        public void StatusCodeHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.STATUS_CODE.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof StatusCodeHandler);
        }

        @Test
        public void QueryStringHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.QUERY_STRING.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof QueryStringHandler);
        }

        @Test
        public void RemoteAddressHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.REMOTE_ADDRESS.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof RemoteAddressHandler);
        }

        @Test
        public void RemoteHostHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.REMOTE_HOST.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof RemoteHostHandler);
        }

        @Test
        public void RemoteUserHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.REMOTE_USER.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof RemoteUserHandler);
        }

        @Test
        public void RequestMethodHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.REQUEST_METHOD.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof RequestMethodHandler);
        }

        @Test
        public void ResponseBytesClfHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.RESPONSE_CLF_BYTES.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof ResponseBytesClfHandler);
        }

        @Test
        public void ResponseBytesHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.RESPONSE_BYTES.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof ResponseBytesHandler);
        }

        @Test
        public void TimeReceivedHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.TIME_RECEIVED.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof TimeReceivedHandler);
        }

        @Test
        public void UrlRequestedHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.URL_REQUESTED.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof UrlRequestedHandler);
        }

        @Test
        public void PercentHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.PERCENT.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof StringHandler);
        }

        @Test
        public void StringHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.STRING.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof StringHandler);
        }

        @Test
        public void ResponseMessageHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.ERROR_MESSAGE.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof ResponseMessageHandler);
        }

        @Test
        public void RequestGuidHandler() {
            final LogArgumentGroupExtractor extractor = LogArgumentGroupExtractor.instance("", "", "", "", LogFormatArgument.TRACE_GUID.toString());

            httpLogFormatter.setLogic(extractor, formatter);

            assertTrue(formatter.getLogic() instanceof TraceGuidHandler);
        }
    }

    public static class WhenEscapingTheMessage {
        private final String escapeThis = "\b\n\t\f\r\\\"'/&<>";
        private final HttpServletRequest request = mock(HttpServletRequest.class);
        private final HttpServletResponseWrapper response = new HttpServletResponseWrapper(
                mock(HttpServletResponse.class),
                ResponseMode.PASSTHROUGH,
                ResponseMode.PASSTHROUGH
        );

        @Before
        public void setup() throws IOException {
            response.sendError(0, escapeThis);
        }

        @Test
        public void EscapeTheMessageForDefault() {
            assertEquals(
                    "\b\n\t\f\r\\\"'/&<>",
                    new HttpLogFormatter("%M").format(request, response)
            );
        }

        @Test
        public void EscapeTheMessageForPlain() {
            assertEquals(
                    "\b\n\t\f\r\\\"'/&<>",
                    new HttpLogFormatter("%M", HttpLogFormatterState.PLAIN).format(request, response)
            );
        }

        @Test
        public void EscapeTheMessageForJson() {
            assertEquals(
                    "\\b\\n\\t\\f\\r\\\\\\\"'\\/&<>",
                    new HttpLogFormatter("%M", HttpLogFormatterState.JSON).format(request, response)
            );
        }

        @Test
        public void EscapeTheMessageForXml() {
            assertEquals(
                    "\n\t\r\\&quot;&apos;/&amp;&lt;&gt;",
                    new HttpLogFormatter("%M", HttpLogFormatterState.XML).format(request, response)
            );
        }
    }
}
