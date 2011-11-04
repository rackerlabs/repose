package com.rackspace.auth.v2_0;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.ws.rs.ext.RuntimeDelegateImpl;

import javax.ws.rs.ext.RuntimeDelegate;

/**
 * @author fran
 */
public class GenericServiceClient {
    static {
        // If this works we need to figure out why and make sure it's part of our init
        RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    }

    private final Client client;

    public GenericServiceClient(String username, String password) {
        DefaultClientConfig cc = new DefaultClientConfig();
        cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
        client = Client.create(cc);

        HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(username, password);
        client.addFilter(authFilter);
    }

    public ServiceClientResponse get(String uri, String... queryParameters) throws AuthServiceException {
        WebResource resource = client.resource(uri);

        if (queryParameters.length % 2 != 0) {
           throw new IllegalArgumentException("Query parameters must be in pairs.");
        }

        for (int index = 0; index < queryParameters.length; index = index + 2) {
           resource = resource.queryParam(queryParameters[index], queryParameters[index + 1]);
        }

        ClientResponse response = resource.header("Accept", "application/xml").get(ClientResponse.class);
        return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
    }
}
