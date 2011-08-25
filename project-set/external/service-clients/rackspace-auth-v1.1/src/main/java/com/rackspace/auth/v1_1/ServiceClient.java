package com.rackspace.auth.v1_1;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;

/**
 * @author fran
 */
public class ServiceClient {
    private final HttpClient client;

    public ServiceClient(String username, String password) {
        this.client = new HttpClient();
        
        final Credentials defaultCredentials = new UsernamePasswordCredentials(username, password);
        client.getState().setCredentials(AuthScope.ANY, defaultCredentials);
    }

    public GetMethod get(String uri, NameValuePair[] queryParameters) throws AuthServiceException {
        final GetMethod getMethod = new GetMethod(uri);
        getMethod.addRequestHeader("Accept", "application/xml");

        if (queryParameters != null) {
            getMethod.setQueryString(queryParameters);
        }

        try {
            client.executeMethod(getMethod);
        } catch (IOException ioe) {
            throw new AuthServiceException("Failed to successfully communicate with Auth v1.1 Service (" + uri + "): "
                    + ioe.getMessage(), ioe);
        }

        return getMethod;
    }
}
