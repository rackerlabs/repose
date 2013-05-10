package com.rackspace.papi.service.proxy.ning;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.*;
import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.http.proxy.HttpException;
import com.rackspace.papi.http.proxy.common.HttpResponseCodeProcessor;
import com.rackspace.papi.commons.util.proxy.ProxyUtilities;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.commons.util.proxy.TargetHostInfo;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("ningRequestProxyService")
public class RequestProxyServiceImpl implements RequestProxyService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProxyServiceImpl.class);
    private static final String ERROR = "Error processing request.";
    private final Object clientLock = new Object();
    private Integer connectionTimeout = Integer.valueOf(0);
    private Integer readTimeout = Integer.valueOf(0);
    private AsyncHttpClient client;
    private boolean rewriteHostHeader = false;

    public RequestProxyServiceImpl() {
        client = new AsyncHttpClient();
    }

    @Override
    public int proxyRequest(String targetHost, HttpServletRequest request, HttpServletResponse response) throws IOException {
        TargetHostInfo host = new TargetHostInfo(targetHost);
        RequestBuilder process = new NingRequestProcessor(request, host).process();
        Request build = process.build();
        ListenableFuture<Response> execute = getClient().prepareRequest(build).execute(new ResponseHandler(response));
        try {
            NingResponseProcessor responseProcessor = new NingResponseProcessor(execute.get(), response);
            HttpResponseCodeProcessor responseCode = new HttpResponseCodeProcessor(response.getStatus());

            if (responseCode.isRedirect()) {
                responseProcessor.sendTranslatedRedirect(response.getStatus());
            } else {
                responseProcessor.process();
            }

            return responseCode.getCode();
        } catch (InterruptedException ex) {
            LOG.warn("Request interrupted", ex);
        } catch (ExecutionException ex) {
            if("ReadLimitReachedException".equals(ex.getCause().getClass().getSimpleName())){
                LOG.error("Error reading request content", ex);
                response.sendError(HttpStatusCode.REQUEST_ENTITY_TOO_LARGE.intValue(), "Error reading request content");
            }else{
                LOG.error(ERROR, ex);
            }
        } catch (HttpException ex) {
            LOG.error(ERROR, ex);
        }
        return -1;
    }

    private AsyncHttpClient getClient() {
        synchronized (clientLock) {
            if (client == null) {
                LOG.info("Building Ning Async Http Client");
                Builder builder = new AsyncHttpClientConfig.Builder();
                builder.setRequestTimeoutInMs(readTimeout);
                builder.setConnectionTimeoutInMs(connectionTimeout);
                builder.setAllowPoolingConnection(true);
                builder.setFollowRedirects(false);
                SSLContext context = ProxyUtilities.getTrustingSslContext();
                if (context != null) {
                    builder.setSSLContext(context);
                }
                client = new AsyncHttpClient(builder.build());
            }
            return client;
        }
    }

    @Override
    public void updateConfiguration(Integer connectionTimeout, Integer readTimeout, Integer proxyThreadPool, boolean requestLogging) {
        LOG.info("Updating Request Proxy configuration");
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;

        // Invalidate client
        synchronized (clientLock) {
            client = null;
        }

    }

    private BoundRequestBuilder setHeader(BoundRequestBuilder builder, Map<String, String> headers) {
       
       final Set<Map.Entry<String, String>> entries = headers.entrySet();
        for(Map.Entry<String,String> entry: entries){
           builder.addHeader(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    private ServiceClientResponse executeRequest(BoundRequestBuilder builder) {
        try {
            Response get = builder.execute().get();
            return new ServiceClientResponse(get.getStatusCode(), get.hasResponseBody() ? get.getResponseBodyAsStream() : null);
        } catch (IOException ex) {
            LOG.error(ERROR, ex);
        } catch (InterruptedException ex) {
            LOG.error(ERROR, ex);
        } catch (ExecutionException ex) {
            LOG.error(ERROR, ex);
        }

        return null;
    }

    @Override
    public ServiceClientResponse get(String uri, Map<String, String> headers) {
        BoundRequestBuilder builder = getClient().prepareGet(uri);
        return executeRequest(setHeader(builder, headers));
    }

    @Override
    public ServiceClientResponse get(String baseUri, String extraUri, Map<String, String> headers) {
        BoundRequestBuilder builder = getClient().prepareGet(StringUriUtilities.appendPath(baseUri, extraUri));
        return executeRequest(setHeader(builder, headers));
    }

    @Override
    public ServiceClientResponse delete(String baseUri, String extraUri, Map<String, String> headers) {
        BoundRequestBuilder builder = getClient().prepareDelete(StringUriUtilities.appendPath(baseUri, extraUri));
        return executeRequest(setHeader(builder, headers));
    }

    @Override
    public ServiceClientResponse put(String uri, Map<String, String> headers, byte[] body) {
        BoundRequestBuilder builder = getClient().preparePut(uri);
        return executeRequest(setHeader(builder.setBody(body), headers));
    }

    @Override
    public ServiceClientResponse put(String baseUri, String path, Map<String, String> headers, byte[] body) {
        BoundRequestBuilder builder = getClient().preparePut(StringUriUtilities.appendPath(baseUri, path));
        return executeRequest(setHeader(builder.setBody(body), headers));
    }

  @Override
  public void setRewriteHostHeader(boolean value) {
    this.rewriteHostHeader = value;
  }
  
  public boolean getRewriteHostHeader() {
    return this.rewriteHostHeader;
  }
}
