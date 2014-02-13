package com.rackspace.papi.service.httpclient.impl

import org.apache.http.Header
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine
import org.apache.http.protocol.HTTP
import org.apache.http.protocol.HttpContext
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.mockito.Mockito.mock


class ConnectionKeepAliveWithTimeoutStrategyTest {

    public static class WhenNoHttpKeepAliveHeader {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 200, "OK"));
        HttpContext context = mock(HttpContext.class);

        @Test
        public void shouldReturnNegativeOneIfTimeoutIsZero() {
            ConnectionKeepAliveWithTimeoutStrategy keepAliveStrategy = new ConnectionKeepAliveWithTimeoutStrategy(0);
            long duration = keepAliveStrategy.getKeepAliveDuration(response, context);
            assertEquals("duration should be -1", duration, -1);
        }

        @Test
        public void shouldReturnConfiguredTimeout() {
            ConnectionKeepAliveWithTimeoutStrategy keepAliveStrategy = new ConnectionKeepAliveWithTimeoutStrategy(1000);
            long duration = keepAliveStrategy.getKeepAliveDuration(response, context);
            assertEquals("duration should be 1000", duration, 1000);
        }
    }

    public static class WhenHttpKeepAliveHeaderExists {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 200, "OK"));
        HttpContext context = mock(HttpContext.class);

        @Test
        public void shouldReturnHttpHeaderValue() {
            response.addHeader(new BasicHeader(HTTP.CONN_KEEP_ALIVE, "timeout=5"));

            ConnectionKeepAliveWithTimeoutStrategy keepAliveStrategy = new ConnectionKeepAliveWithTimeoutStrategy(2);
            long duration = keepAliveStrategy.getKeepAliveDuration(response, context);
            assertEquals("duration should be 5000", 5000, duration);
        }

    }

}