package com.rackspace.papi.service.proxy.ning;

import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;
import com.rackspace.papi.http.proxy.HttpException;
import com.rackspace.papi.http.proxy.common.AbstractResponseProcessor;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

public class NingResponseProcessor extends AbstractResponseProcessor {

    private Response clientResponse;

    public NingResponseProcessor(String proxiedHostUrl, String requestHostPath, Response clientResponse, HttpServletResponse response) {
        super(proxiedHostUrl, requestHostPath, response, clientResponse.getStatusCode());
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

    @Override
    protected String getResponseHeaderValue(String headerName) {
        final List<String> headerValues = clientResponse.getHeaders().get(headerName);
        if (headerValues == null || headerValues.isEmpty()) {
            return null;
        }

        return headerValues.get(0);
    }
}
