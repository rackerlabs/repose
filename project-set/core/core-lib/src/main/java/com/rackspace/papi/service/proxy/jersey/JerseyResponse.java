package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.commons.util.servlet.http.ProxiedResponse;
import com.sun.jersey.api.client.ClientResponse;
import java.io.IOException;
import java.io.InputStream;

public class JerseyResponse implements ProxiedResponse {
    private final ClientResponse response;
    
    public JerseyResponse(ClientResponse response) {
        this.response = response;
    }
    
    @Override
    public InputStream getInputStream() {
        return response.getEntityInputStream();
    }

    @Override
    public void close() throws IOException {
        InputStream input = response.getEntityInputStream();
        if (input != null) {
            input.close();
        }

        response.close();
    }
    
}
