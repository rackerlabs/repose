package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.http.proxy.HttpException;
import com.rackspace.papi.http.proxy.common.HttpResponseCodeProcessor;
import com.rackspace.papi.commons.util.proxy.ProxyRequestException;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.commons.util.proxy.TargetHostInfo;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("jerseyRequestProxyService")
public class RequestProxyServiceImpl implements RequestProxyService {

    private static final Integer DEFAULT_THREADPOOL_SIZE = 20;
    private static final Logger LOG = LoggerFactory.getLogger(RequestProxyServiceImpl.class);
    private ClientWrapper client;
    private Integer connectionTimeout = Integer.valueOf(0);
    private Integer readTimeout = Integer.valueOf(0);
    private Integer proxyThreadPool = DEFAULT_THREADPOOL_SIZE;
    private final Object clientLock = new Object();
    private boolean requestLogging;
    private final ReposeInstanceInfo instanceInfo;
    private boolean rewriteHostHeader = false;

    @Autowired
    public RequestProxyServiceImpl(@Qualifier("reposeInstanceInfo") ReposeInstanceInfo instanceInfo) {
        this.instanceInfo = instanceInfo;
    }

    @Override
    public void updateConfiguration(Integer connectionTimeout, Integer readTimeout, Integer proxyThreadPool, boolean requestLogging) {
        LOG.info("Updating Request Proxy configuration");
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.proxyThreadPool = proxyThreadPool;
        this.requestLogging = requestLogging;

        // Invalidate client
        synchronized (clientLock) {
            client = null;
        }
    }

    private ClientWrapper getClient() {
        synchronized (clientLock) {
            if (client == null) {
                LOG.info("Building Jersey Http Client");
                JerseyPropertiesConfigurator jerseyPropertiesConfigurator = new JerseyPropertiesConfigurator(connectionTimeout, readTimeout, proxyThreadPool);
                URLConnectionClientHandler urlConnectionClientHandler = new URLConnectionClientHandler(new ReposeHttpUrlConnectionFactory());
                client = new ClientWrapper(new Client(urlConnectionClientHandler, jerseyPropertiesConfigurator.configure()), requestLogging, instanceInfo);
            }

            return client;
        }
    }

    @Override
    public int proxyRequest(String targetHost, HttpServletRequest request, HttpServletResponse response) throws IOException {
        TargetHostInfo host = new TargetHostInfo(targetHost);
        final String target = host.getProxiedHostUrl().toExternalForm() + request.getRequestURI();

        JerseyRequestProcessor processor = new JerseyRequestProcessor(request, host.getProxiedHostUri(), rewriteHostHeader);
        try {
            WebResource resource = getClient().resource(target);
            WebResource.Builder builder = processor.process(resource);
            return executeProxyRequest(builder, request, response);
        } catch (HttpException ex) {
            LOG.error("Error processing request", ex);
            MutableHttpServletResponse mutableResponse = (MutableHttpServletResponse) response;
            mutableResponse.setLastException(ex);
        }

        return -1;
    }

    private int executeProxyRequest(WebResource.Builder builder, HttpServletRequest sourceRequest, HttpServletResponse sourceResponse) throws IOException, HttpException {

        ClientResponse response = builder.method(sourceRequest.getMethod(), ClientResponse.class);

        HttpResponseCodeProcessor responseCode = new HttpResponseCodeProcessor(response.getStatus());
        JerseyResponseProcessor responseProcessor = new JerseyResponseProcessor(response, sourceResponse);

        if (responseCode.isRedirect()) {
            responseProcessor.sendTranslatedRedirect(response.getStatus());
        } else {
            responseProcessor.process();
        }

        return responseCode.getCode();
    }

    private WebResource.Builder setHeaders(WebResource.Builder builder, Map<String, String> headers) {
        WebResource.Builder newBuilder = builder;
        
        final Set<Entry<String, String>> entries = headers.entrySet();
        for(Entry<String,String> entry: entries){
           newBuilder = newBuilder.header(entry.getKey(), entry.getValue());
        }
        
        return newBuilder;
    }

    @Override
    public ServiceClientResponse get(String uri, Map<String, String> headers) {
        WebResource.Builder requestBuilder = getClient().resource(uri).getRequestBuilder();
        try {
            ClientResponse response = setHeaders(requestBuilder, headers).get(ClientResponse.class);
            return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
        } catch (ClientHandlerException ex) {
            throw new ProxyRequestException("Error dispatching GET request", ex);
        }
    }

    @Override
    public ServiceClientResponse get(String baseUri, String path, Map<String, String> headers) {
        WebResource.Builder requestBuilder = getClient().resource(baseUri, true).path(path).getRequestBuilder();
        try {
            ClientResponse response = setHeaders(requestBuilder, headers).get(ClientResponse.class);
            return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
        } catch (ClientHandlerException ex) {
            throw new ProxyRequestException("Error dispatching GET request", ex);
        }
    }

    @Override
    public ServiceClientResponse delete(String baseUri, String path, Map<String, String> headers) {
        WebResource.Builder requestBuilder = getClient().resource(baseUri, true).path(path).getRequestBuilder();
        try {
            ClientResponse response = setHeaders(requestBuilder, headers).delete(ClientResponse.class);
            return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
        } catch (ClientHandlerException ex) {
            throw new ProxyRequestException("Error dispatching DELETE request", ex);
        }
    }

    @Override
    public ServiceClientResponse put(String uri, Map<String, String> headers, byte[] body) {
        WebResource resource = getClient().resource(uri);
        try {
            ClientResponse response = setHeaders(resource.getRequestBuilder(), headers).put(ClientResponse.class, body);
            return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
        } catch (ClientHandlerException ex) {
            throw new ProxyRequestException("Error dispatching PUT request", ex);
        }
    }

    @Override
    public ServiceClientResponse put(String baseUri, String path, Map<String, String> headers, byte[] body) {
        WebResource resource = getClient().resource(baseUri, true).path(path);
        try {
            ClientResponse response = setHeaders(resource.getRequestBuilder(), headers).put(ClientResponse.class, body);

            return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
        } catch (ClientHandlerException ex) {
            throw new ProxyRequestException("Error dispatching PUT request", ex);
        }
    }

  @Override
  public void setRewriteHostHeader(boolean value) {
    rewriteHostHeader = value;
  }
}