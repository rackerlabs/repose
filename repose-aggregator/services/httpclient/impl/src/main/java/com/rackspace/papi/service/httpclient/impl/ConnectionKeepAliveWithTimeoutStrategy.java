package com.rackspace.papi.service.httpclient.impl;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Custom keep alive strategy that defaults to the non-standard Keep-Alive header to communicate to the client the
 * period of time in seconds they intend to keep the connection alive on the server side.  If this
 * header is present in the response, the value in this header will be used to determine the maximum
 * length of time to keep a persistent connection open for.
 *
 * If the Keep-Alive header is NOT present in the response, the value of keepalive.timeout is
 * evaluated.  If this value is 0, the connection will be kept alive indefinitely.  If the value is
 * greater than 0, the connection will be kept alive for the number of milliseconds specified.
 */
public class ConnectionKeepAliveWithTimeoutStrategy extends DefaultConnectionKeepAliveStrategy {

    private int timeout;

    public ConnectionKeepAliveWithTimeoutStrategy(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        long duration = super.getKeepAliveDuration(response, context);

        if (duration > 0) {
            return duration;
        }

        return timeout == 0 ? -1 : timeout;
    }
}
