package com.rackspace.papi.commons.util.http;


import com.rackspace.papi.commons.util.logging.jersey.LoggingFilter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import javax.net.ssl.*;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

import com.sun.jersey.client.urlconnection.HTTPSProperties;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fran
 */
public class ServiceClient {
    private static final String ACCEPT_HEADER = "Accept";
    private static final String MEDIA_TYPE = "application/xml";
    private static final Logger LOG = LoggerFactory.getLogger(ServiceClient.class);
    private static final int TIMEOUT = 30000;

    private final Client client;

    public ServiceClient(Client client) {
        this.client = client;
    }

    public ServiceClient(String username, String password) {
        this(new HTTPBasicAuthFilter(username, password));
    }

    public ServiceClient() {
        this((HTTPBasicAuthFilter) null);
    }

    public ServiceClient(HTTPBasicAuthFilter httpBasicAuthFilter) {
        DefaultClientConfig cc = new DefaultClientConfig();
        cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
        cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, TIMEOUT);
        cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, TIMEOUT);

        try {
            // Use the default SSLContext since we use Jersey with HttpsURLConnection.  HttpsURLConnection
            // settings are system-wide (not client-wide).  So let the core Repose set these based on a
            // configuration variable and use the default here.
            cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(new ReposeHostnameVerifier(), SSLContext.getDefault()));
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Error when setting Jersey HTTPS properties", e);
        }

        client = Client.create(cc);

        if (httpBasicAuthFilter != null) {
            client.addFilter(httpBasicAuthFilter);
        }

        if (LOG.isDebugEnabled() || LOG.isTraceEnabled()) {
            LOG.info("Enabling info logging of Rackspace Auth v1.1 client requests");
            client.addFilter(new LoggingFilter());
        }
    }

    private static class ReposeHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession sslSession) {
            LOG.info("verifying: " + hostname);
            return true;
        }
    }

    private WebResource.Builder setHeaders(WebResource.Builder builder, Map<String, String> headers) {
        WebResource.Builder newBuilder = builder;

        final Set<Entry<String, String>> entries = headers.entrySet();
        
        for(Entry<String, String> entry: entries){
           newBuilder = newBuilder.header(entry.getKey(), entry.getValue());
        }
        
        return newBuilder;
    }

    public ServiceClientResponse post(String uri, JAXBElement body, MediaType contentType) {
        WebResource resource = client.resource(uri);

        ClientResponse response = resource.type(contentType).header(ACCEPT_HEADER, MEDIA_TYPE).post(ClientResponse.class, body);

        return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
    }

    public ServiceClientResponse get(String uri, Map<String, String> headers, String... queryParameters) {
        WebResource resource = client.resource(uri);

        if (queryParameters.length % 2 != 0) {
            throw new IllegalArgumentException("Query parameters must be in pairs.");
        }

        for (int index = 0; index < queryParameters.length; index = index + 2) {
            resource = resource.queryParam(queryParameters[index], queryParameters[index + 1]);
        }

        WebResource.Builder requestBuilder = resource.getRequestBuilder();
        requestBuilder = setHeaders(requestBuilder, headers);
        ClientResponse response = requestBuilder.get(ClientResponse.class);
        return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
    }

}
