package com.rackspace.papi.service.proxy.ning;

import com.ning.http.client.*;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ResponseHandler implements AsyncHandler<Response> {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseHandler.class);
    private final Response.ResponseBuilder builder;
    private final HttpServletResponse response;

    public ResponseHandler(HttpServletResponse response) {
        this(response, new Response.ResponseBuilder());
    }
    
    ResponseHandler(HttpServletResponse response, Response.ResponseBuilder builder) {
        this.builder = builder;
        this.response = response;
    }

    @Override
    public void onThrowable(Throwable throwable) {
        LOG.warn("Error reading response", throwable);
    }

    @Override
    public AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart part) throws IOException {
        part.writeTo(response.getOutputStream());
        return AsyncHandler.STATE.CONTINUE;
    }

    @Override
    public AsyncHandler.STATE onStatusReceived(HttpResponseStatus status) {
        builder.accumulate(status);
        return AsyncHandler.STATE.CONTINUE;
    }

    @Override
    public AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers) {
        builder.accumulate(headers);
        return AsyncHandler.STATE.CONTINUE;
    }

    @Override
    public Response onCompleted() {
        return builder.build();
    }
}
