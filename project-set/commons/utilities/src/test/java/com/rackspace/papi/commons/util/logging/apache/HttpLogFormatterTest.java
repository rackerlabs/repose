/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package com.rackspace.papi.commons.util.logging.apache;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;
import com.rackspace.papi.commons.util.logging.apache.format.LogArgumentFormatter;
import com.rackspace.papi.commons.util.logging.apache.format.stock.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class HttpLogFormatterTest {

    public static HttpLogFormatter newFormatter(String format) {
        return new HttpLogFormatter(format);
    }

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
            final HttpLogFormatter formatter = newFormatter("%h %% %u %U");

            assertTrue("Should have parsed seven handlers. Only found: " + formatter.getHandlerList().size(), formatter.getHandlerList().size() == 7);
        }

        @Test
        public void shouldCorrectlyParseEmptySpace() {
            final HttpLogFormatter formatter = newFormatter("%h%%%u%U");

            assertTrue("Should have parsed four handlers. Only found: " + formatter.getHandlerList().size(), formatter.getHandlerList().size() == 4);
        }

        @Test
        public void shouldPreserveStringFormatting() {
            final HttpLogFormatter formatter = newFormatter("%%log output%% %U");
            final String expected = "%log output% http://some.place.net/u/r/l";

            assertEquals(expected, formatter.format(request, response));
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
            final HttpLogFormatter formatter = newFormatter("%200,201U");
            final String expected = "http://some.place.net/u/r/l";

            when(response.getStatus()).thenReturn(200);
            assertEquals(expected, formatter.format(request, response));
            when(response.getStatus()).thenReturn(401);
            assertEquals("-", formatter.format(request, response));
        }

        @Test
        public void shouldParseExclusiveStatusCodeRestrictions() {
            final HttpLogFormatter formatter = newFormatter("%!401,403U");
            final String expected = "http://some.place.net/u/r/l";

            assertEquals(expected, formatter.format(request, response));
            when(response.getStatus()).thenReturn(401);
            assertEquals("-", formatter.format(request, response));
        }
    }

    public static class WhenSettingLogic {
        private String lastEntity;
        private LogArgumentFormatter formatter;
        private FormatterLogic logic;

        @Before
        public void setup() {
            lastEntity = null;

            formatter = new LogArgumentFormatter();
        }

        @Test
        public void shouldNotHaveSetLogicByDefault() {
            assertNull(formatter.getLogic());
        }

        @Test
        public void CanonicalPortHandler() {
            lastEntity = LogFormatArgument.CANONICAL_PORT.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof CanonicalPortHandler);
        }

        @Test
        public void LocalAddressHandler() {
            lastEntity = LogFormatArgument.LOCAL_ADDRESS.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof LocalAddressHandler);
        }

        @Test
        public void StatusCodeHandler() {
            lastEntity = LogFormatArgument.STATUS_CODE.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof StatusCodeHandler);
        }

        @Test
        public void QueryStringHandler() {
            lastEntity = LogFormatArgument.QUERY_STRING.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof QueryStringHandler);
        }

        @Test
        public void RemoteAddressHandler() {
            lastEntity = LogFormatArgument.REMOTE_ADDRESS.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof RemoteAddressHandler);
        }

        @Test
        public void RemoteHostHandler() {
            lastEntity = LogFormatArgument.REMOTE_HOST.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof RemoteHostHandler);
        }

        @Test
        public void RemoteUserHandler() {
            lastEntity = LogFormatArgument.REMOTE_USER.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof RemoteUserHandler);
        }

        @Test
        public void RequestMethodHandler() {
            lastEntity = LogFormatArgument.REQUEST_METHOD.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof RequestMethodHandler);
        }

        @Test
        public void ResponseBytesHandler() {
            lastEntity = LogFormatArgument.RESPONSE_BYTES.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof ResponseBytesHandler);
        }

        @Test
        public void TimeReceivedHandler() {
            lastEntity = LogFormatArgument.TIME_RECIEVED.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof TimeReceivedHandler);
        }

        @Test
        public void UrlRequestedHandler() {
            lastEntity = LogFormatArgument.URL_REQUESTED.toString();

            HttpLogFormatter.setLogic(lastEntity, formatter);

            assertTrue(formatter.getLogic() instanceof UrlRequestedHandler);
        }
     }
}
