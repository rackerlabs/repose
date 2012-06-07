package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.logging.jersey.LoggingFilter;
import com.rackspace.papi.http.proxy.HttpException;
import com.rackspace.papi.http.proxy.common.HttpResponseCodeProcessor;
import com.rackspace.papi.service.proxy.RequestProxyService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("requestProxyService")
public class RequestProxyServiceImpl implements RequestProxyService {

    private static final Integer DEFAULT_THREADPOOL_SIZE = 20;
    private static final Logger LOG = LoggerFactory.getLogger(RequestProxyServiceImpl.class);
    private ClientWrapper client;
    private Integer connectionTimeout = Integer.valueOf(0);
    private Integer readTimeout = Integer.valueOf(0);
    private Integer proxyThreadPool = Integer.valueOf(DEFAULT_THREADPOOL_SIZE);
    private final Object clientLock = new Object();
    private boolean requestLogging;

    public RequestProxyServiceImpl() {
    }

    @Override
    public void setTimeouts(Integer connectionTimeout, Integer readTimeout, Integer proxyThreadPool, boolean requestLogging) {
        LOG.info("Connection and/or read timeouts changed to: " + connectionTimeout + "/" + readTimeout);
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.proxyThreadPool = proxyThreadPool;
        this.requestLogging = requestLogging;

        // Invalidate client
        synchronized (clientLock) {
            client = null;
        }
    }

    private static class TargetHostInfo {

        private final URI proxiedHostUri;
        private final URL proxiedHostUrl;

        public TargetHostInfo(String targetHost) {
            URI targetUri = null;

            try {
                targetUri = new URI(targetHost);
            } catch (URISyntaxException e) {
                LOG.error("Invalid target host url: " + targetHost, e);
            }

            proxiedHostUri = targetUri;
            proxiedHostUrl = asUri(proxiedHostUri);

        }

        private URL asUri(URI host) {
            try {
                return new URL(host.getScheme(), host.getHost(), host.getPort(), "");
            } catch (MalformedURLException ex) {
                LOG.error("Invalid host url: " + host, ex);
            }
            return null;
        }

        public URI getProxiedHostUri() {
            return proxiedHostUri;
        }

        public URL getProxiedHostUrl() {
            return proxiedHostUrl;
        }
    }
    
    private ClientWrapper getClient() {
        synchronized (clientLock) {
            if (client == null) {
                JerseyPropertiesConfigurator jerseyPropertiesConfigurator = new JerseyPropertiesConfigurator(connectionTimeout, readTimeout, proxyThreadPool, true);
                client = new ClientWrapper(Client.create(jerseyPropertiesConfigurator.configure()));

                if (requestLogging) {
                    LOG.warn("Enabling info logging of jersey client requests");
                    client.getClient().addFilter(new LoggingFilter());
                } else {
                    LOG.warn("**** Jersey client request logging not enabled *****");
                }

            }

            return client;
        }
    }

    @Override
    public int proxyRequest(String targetHost, HttpServletRequest request, HttpServletResponse response) throws IOException {
        TargetHostInfo host = new TargetHostInfo(targetHost);
        final String target = host.getProxiedHostUrl().toExternalForm() + request.getRequestURI();

        JerseyRequestProcessor processor = new JerseyRequestProcessor(request, host.getProxiedHostUri());
        WebResource resource = getClient().resource(target);
        WebResource.Builder builder = processor.process(resource);
        try {
            return executeProxyRequest(host, builder, request, response);
        } catch (HttpException ex) {
            LOG.error("Error processing request", ex);
        }

        return -1;
    }

    private String extractHostPath(HttpServletRequest request) {
        final StringBuilder myHostName = new StringBuilder(request.getServerName());

        if (request.getServerPort() != 80) {
            myHostName.append(":").append(request.getServerPort());
        }

        return myHostName.append(request.getContextPath()).toString();
    }

    private int executeProxyRequest(TargetHostInfo host, WebResource.Builder builder, HttpServletRequest sourceRequest, HttpServletResponse sourceResponse) throws IOException, HttpException {

        ClientResponse response = builder.method(sourceRequest.getMethod(), ClientResponse.class);

        HttpResponseCodeProcessor responseCode = new HttpResponseCodeProcessor(response.getStatus());
        JerseyResponseProcessor responseProcessor = new JerseyResponseProcessor(response, sourceResponse);

        if (responseCode.isRedirect()) {
            responseProcessor.sendTranslatedRedirect(host.getProxiedHostUrl().toExternalForm(), extractHostPath(sourceRequest), response.getStatus());
        } else {
            responseProcessor.process();
        }

        return responseCode.getCode();
    }

    private WebResource.Builder setHeaders(WebResource.Builder builder, Map<String, String> headers) {
        WebResource.Builder newBuilder = builder;

        for (String key : headers.keySet()) {
            newBuilder = newBuilder.header(key, headers.get(key));
        }

        return builder;
    }

    @Override
    public ServiceClientResponse get(String uri, Map<String, String> headers) {
        WebResource.Builder requestBuilder = getClient().resource(uri).getRequestBuilder();
        ClientResponse response = setHeaders(requestBuilder, headers).get(ClientResponse.class);
        return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
    }

    @Override
    public ServiceClientResponse get(String baseUri, String path, Map<String, String> headers) {
        WebResource.Builder requestBuilder = getClient().resource(baseUri, true).path(path).getRequestBuilder();
        ClientResponse response = setHeaders(requestBuilder, headers).get(ClientResponse.class);
        return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
    }

    @Override
    public ServiceClientResponse delete(String baseUri, String path, Map<String, String> headers) {
        WebResource.Builder requestBuilder = getClient().resource(baseUri, true).path(path).getRequestBuilder();
        ClientResponse response = setHeaders(requestBuilder, headers).delete(ClientResponse.class);
        return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
    }

    @Override
    public ServiceClientResponse put(String uri, Map<String, String> headers, byte[] body) {
        WebResource resource = getClient().resource(uri);
        ClientResponse response = setHeaders(resource.getRequestBuilder(), headers).put(ClientResponse.class, body);
        return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
    }

    @Override
    public ServiceClientResponse put(String baseUri, String path, Map<String, String> headers, byte[] body) {
        WebResource resource = getClient().resource(baseUri, true).path(path);
        ClientResponse response = setHeaders(resource.getRequestBuilder(), headers).put(ClientResponse.class, body);

        return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
    }
}
