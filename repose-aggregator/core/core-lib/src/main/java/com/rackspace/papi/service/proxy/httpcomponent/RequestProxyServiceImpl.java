package com.rackspace.papi.service.proxy.httpcomponent;

import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.proxy.ProxyRequestException;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.http.proxy.HttpException;
import com.rackspace.papi.service.httpclient.HttpClientNotFoundException;
import com.rackspace.papi.service.httpclient.HttpClientService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

public class RequestProxyServiceImpl implements RequestProxyService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProxyServiceImpl.class);
    private boolean rewriteHostHeader = false;
    private static final String CHUNKED_ENCODING_PARAM = "chunked-encoding";

    private HttpClientService httpClientService;

    private HttpHost getProxiedHost(String targetHost) throws HttpException {
        try {
            return URIUtils.extractHost(new URI(targetHost));
        } catch (URISyntaxException ex) {
            LOG.error("Invalid target host url: " + targetHost, ex);
        }

        throw new HttpException("Invalid target host");
    }

    public RequestProxyServiceImpl() {
    }

    private HttpClient getClient() {
        try {
            HttpClient httpClient = httpClientService.getClient(null).getHttpClient();
            return httpClient;
        } catch (HttpClientNotFoundException e) {
            LOG.error("Failed to obtain an HTTP default client connection");
            throw new ProxyRequestException("Failed to obtain an HTTP default client connection", e);
        }
    }

    @Override
    public int proxyRequest(String targetHost, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {

            final boolean isChunkedConfigured = getClient().getParams().getBooleanParameter(CHUNKED_ENCODING_PARAM, true);
            final HttpHost proxiedHost = getProxiedHost(targetHost);
            final String target = proxiedHost.toURI() + request.getRequestURI();
            final HttpComponentRequestProcessor processor = new HttpComponentRequestProcessor(request, new URI(proxiedHost.toURI()), rewriteHostHeader,isChunkedConfigured);
            final HttpComponentProcessableRequest method = HttpComponentFactory.getMethod(request.getMethod(), processor.getUri(target));

            if (method != null) {
                HttpRequestBase processedMethod = method.process(processor);

                return executeProxyRequest(processedMethod, response);
            }
        } catch (URISyntaxException ex) {
            LOG.error("Error processing request", ex);
        } catch (HttpException ex) {
            LOG.error("Error processing request", ex);
        }

        //Something exploded; return a status code that doesn't exist
        return -1;
    }

    private int executeProxyRequest(HttpRequestBase httpMethodProxyRequest, HttpServletResponse response) throws IOException, HttpException {

        try {


            HttpResponse httpResponse = getClient().execute(httpMethodProxyRequest);
            HttpComponentResponseCodeProcessor responseCode = new HttpComponentResponseCodeProcessor(httpResponse.getStatusLine().getStatusCode());
            HttpComponentResponseProcessor responseProcessor = new HttpComponentResponseProcessor(httpResponse, response, responseCode);

            if (responseCode.isRedirect()) {
                responseProcessor.sendTranslatedRedirect(responseCode.getCode());
            } else {
                responseProcessor.process();
            }

            return responseCode.getCode();
        } catch (ClientProtocolException ex) {
            if ("ReadLimitReachedException".equals(ex.getCause().getCause().getClass().getSimpleName())) {
                LOG.error("Error reading request content", ex);
                response.sendError(HttpStatusCode.REQUEST_ENTITY_TOO_LARGE.intValue(), "Error reading request content");
            } else {
                LOG.error("Error processing request", ex);
                return -1;
            }
        }
        return 1;

    }

    private void setHeaders(HttpRequestBase base, Map<String, String> headers) {

        final Set<Map.Entry<String, String>> entries = headers.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            base.addHeader(entry.getKey(), entry.getValue());
        }
    }

    private ServiceClientResponse execute(HttpRequestBase base) {
        try {
            HttpResponse httpResponse = getClient().execute(base);
            HttpEntity entity = httpResponse.getEntity();
            HttpComponentResponseCodeProcessor responseCode = new HttpComponentResponseCodeProcessor(httpResponse.getStatusLine().getStatusCode());

            InputStream stream = null;
            if (entity != null) {
                stream = new ByteArrayInputStream(RawInputStreamReader.instance().readFully(entity.getContent()));
                EntityUtils.consume(entity);
            }

            return new ServiceClientResponse(responseCode.getCode(), stream);
        } catch (IOException ex) {
            LOG.error("Error executing request", ex);
        } finally {
            base.releaseConnection();
        }

        return new ServiceClientResponse(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue(), null);
    }

    @Override
    public ServiceClientResponse get(String uri, Map<String, String> headers) {
        HttpGet get = new HttpGet(uri);
        setHeaders(get, headers);
        return execute(get);
    }

    @Override
    public ServiceClientResponse get(String baseUri, String extraUri, Map<String, String> headers) {
        HttpGet get = new HttpGet(StringUriUtilities.appendPath(baseUri, extraUri));
        setHeaders(get, headers);
        return execute(get);
    }

    @Override
    public ServiceClientResponse delete(String baseUri, String extraUri, Map<String, String> headers) {
        HttpDelete delete = new HttpDelete(StringUriUtilities.appendPath(baseUri, extraUri));
        setHeaders(delete, headers);
        return execute(delete);
    }

    @Override
    public ServiceClientResponse put(String uri, Map<String, String> headers, byte[] body) {
        HttpPut put = new HttpPut(uri);
        setHeaders(put, headers);
        if (body != null && body.length > 0) {
            put.setEntity(new InputStreamEntity(new ByteArrayInputStream(body), body.length));
        }
        return execute(put);
    }

    @Override
    public ServiceClientResponse put(String baseUri, String path, Map<String, String> headers, byte[] body) {
        HttpPut put = new HttpPut(StringUriUtilities.appendPath(baseUri, path));
        setHeaders(put, headers);
        if (body != null && body.length > 0) {
            put.setEntity(new InputStreamEntity(new ByteArrayInputStream(body), body.length));
        }
        return execute(put);
    }

    @Override
    public void setRewriteHostHeader(boolean value) {
        this.rewriteHostHeader = value;
    }

    public void setHttpClientService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }
}
