package com.rackspace.papi.service.proxy.ning;

import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;
import com.rackspace.papi.http.proxy.common.AbstractResponseProcessor;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

public class NingResponseProcessor extends AbstractResponseProcessor {

    private Response clientResponse;

    public NingResponseProcessor(Response clientResponse, HttpServletResponse response) {
        super(response, clientResponse.getStatusCode());
        this.clientResponse = clientResponse;
    }

    @Override
    protected void setResponseHeaders() throws IOException {
        FluentCaseInsensitiveStringsMap headers = clientResponse.getHeaders();
        for (String headerName : headers.keySet()) {
            for (String value : headers.get(headerName)) {
                addHeader(headerName, value);
            }
        }
    }

    @Override
    protected void setResponseBody() throws IOException {
    }
}
