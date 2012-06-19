package com.rackspace.papi.service.proxy.ning;

import com.ning.http.client.*;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseHandler implements AsyncHandler<Response> {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseHandler.class);
    private final Response.ResponseBuilder builder;
    private final HttpServletResponse response;
    private AsyncHttpClient client;

    public ResponseHandler(HttpServletResponse response) {
        this.builder = new Response.ResponseBuilder();
        this.response = response;
    }

    @Override
    public void onThrowable(Throwable throwable) {
        LOG.warn("Error reading response", throwable);
    }

    @Override
    public AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart part) throws Exception {
        part.writeTo(response.getOutputStream());
        return AsyncHandler.STATE.CONTINUE;
    }

    @Override
    public AsyncHandler.STATE onStatusReceived(HttpResponseStatus status) throws Exception {
        builder.accumulate(status);
        return AsyncHandler.STATE.CONTINUE;
    }

    @Override
    public AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        builder.accumulate(headers);
        return AsyncHandler.STATE.CONTINUE;
    }

    @Override
    public Response onCompleted() throws Exception {
        return builder.build();
    }
}
