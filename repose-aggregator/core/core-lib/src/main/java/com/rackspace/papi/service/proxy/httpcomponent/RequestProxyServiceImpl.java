package com.rackspace.papi.service.proxy.httpcomponent;

import com.google.common.base.Optional;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.proxy.ProxyRequestException;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.http.proxy.HttpException;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.Severity;
import com.rackspace.papi.service.httpclient.HttpClientNotFoundException;
import com.rackspace.papi.service.httpclient.HttpClientResponse;
import com.rackspace.papi.service.httpclient.HttpClientService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

@Component
public class RequestProxyServiceImpl implements RequestProxyService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProxyServiceImpl.class);
    private final ConfigurationService configurationService;
    private final SystemModelInterrogator systemModelInterrogator;
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";

    private boolean rewriteHostHeader = false;
    private static final String CHUNKED_ENCODING_PARAM = "chunked-encoding";

    private HttpClientService httpClientService;
    private HealthCheckService.HealthCheckServiceProxy healthCheckServiceProxy;
    private ContainerConfigListener configListener;
    private SystemModelListener systemModelListener;

    @Autowired
    public RequestProxyServiceImpl(ConfigurationService configurationService,
                                   SystemModelInterrogator systemModelInterrogator,
                                   HealthCheckService healthCheckService) {
        this.configurationService = configurationService;
        this.systemModelInterrogator = systemModelInterrogator;
        this.healthCheckServiceProxy = healthCheckService.register(RequestProxyServiceImpl.class);
    }

    @PostConstruct
    public void afterPropertiesSet() {
        this.configListener = new ContainerConfigListener();
        this.systemModelListener = new SystemModelListener();

        configurationService.subscribeTo("container.cfg.xml", configListener, ContainerConfiguration.class);
        configurationService.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        if (configurationService != null) {
            configurationService.unsubscribeFrom("container.cfg.xml", configListener);
            configurationService.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
        }
    }

    private class ContainerConfigListener implements UpdateListener<ContainerConfiguration> {
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration config) {
            Integer connectionTimeout = config.getDeploymentConfig().getConnectionTimeout();
            Integer readTimeout = config.getDeploymentConfig().getReadTimeout();
            Integer proxyThreadPool = config.getDeploymentConfig().getProxyThreadPool();
            boolean requestLogging = config.getDeploymentConfig().isClientRequestLogging();
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    private class SystemModelListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel config) {
            Optional<ReposeCluster> localCluster = systemModelInterrogator.getLocalCluster(config);

            if (localCluster.isPresent()) {
                setRewriteHostHeader(localCluster.get().isRewriteHostHeader());
                isInitialized = true;

                healthCheckServiceProxy.resolveIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT);
            } else {
                LOG.error("Unable to identify the local host in the system model - please check your system-model.cfg.xml");
                healthCheckServiceProxy.reportIssue(SYSTEM_MODEL_CONFIG_HEALTH_REPORT, "Unable to identify the " +
                        "local host in the system model - please check your system-model.cfg.xml", Severity.BROKEN);
            }
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }


    private HttpHost getProxiedHost(String targetHost) throws HttpException {
        try {
            return URIUtils.extractHost(new URI(targetHost));
        } catch (URISyntaxException ex) {
            LOG.error("Invalid target host url: " + targetHost, ex);
        }

        throw new HttpException("Invalid target host");
    }


    private HttpClientResponse getClient() {
        try {
            HttpClientResponse httpClientResponse = httpClientService.getClient(null);
            return httpClientResponse;
        } catch (HttpClientNotFoundException e) {
            LOG.error("Failed to obtain an HTTP default client connection");
            throw new ProxyRequestException("Failed to obtain an HTTP default client connection", e);
        }
    }

    @Override
    public int proxyRequest(String targetHost, HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpClientResponse httpClientResponse = getClient();

        try {
            final boolean isChunkedConfigured = httpClientResponse.getHttpClient().getParams().getBooleanParameter(CHUNKED_ENCODING_PARAM, true);
            final HttpHost proxiedHost = getProxiedHost(targetHost);
            final String target = proxiedHost.toURI() + request.getRequestURI();
            final HttpComponentRequestProcessor processor = new HttpComponentRequestProcessor(request, new URI(proxiedHost.toURI()), rewriteHostHeader, isChunkedConfigured);
            final HttpComponentProcessableRequest method = HttpComponentFactory.getMethod(request.getMethod(), processor.getUri(target));

            if (method != null) {
                HttpRequestBase processedMethod = method.process(processor);

                return executeProxyRequest(processedMethod, response);
            }
        } catch (URISyntaxException | HttpException ex) {
            LOG.error("Error processing request", ex);
        } finally {
            httpClientService.releaseClient(httpClientResponse);
        }

        //Something exploded; return a status code that doesn't exist
        return -1;
    }

    private int executeProxyRequest(HttpRequestBase httpMethodProxyRequest, HttpServletResponse response) throws IOException, HttpException {
        HttpClientResponse httpClientResponse = getClient();

        try {
            HttpResponse httpResponse = httpClientResponse.getHttpClient().execute(httpMethodProxyRequest);
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
        } finally {
            httpClientService.releaseClient(httpClientResponse);
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
        HttpClientResponse httpClientResponse = getClient();
        try {
            HttpResponse httpResponse = httpClientResponse.getHttpClient().execute(base);
            HttpEntity entity = httpResponse.getEntity();
            HttpComponentResponseCodeProcessor responseCode = new HttpComponentResponseCodeProcessor(httpResponse.getStatusLine().getStatusCode());

            InputStream stream = null;
            if (entity != null) {
                stream = new ByteArrayInputStream(RawInputStreamReader.instance().readFully(entity.getContent()));
                EntityUtils.consume(entity);
            }

            return new ServiceClientResponse(responseCode.getCode(), stream);
        } catch (IOException ex) {
            LOG.error("Error executing request to {}", base.getURI().toString(), ex);
        } finally {
            base.releaseConnection();
            httpClientService.releaseClient(httpClientResponse);
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

    //todo: chain these
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
    public ServiceClientResponse patch(String baseUri, String path, Map<String, String> headers, byte[] body) {
        HttpPatch patch = new HttpPatch(StringUriUtilities.appendPath(baseUri, path));
        setHeaders(patch, headers);
        if (body != null && body.length > 0) {
            patch.setEntity(new InputStreamEntity(new ByteArrayInputStream(body), body.length));
        }
        return execute(patch);
    }

    @Override
    public void setRewriteHostHeader(boolean value) {
        this.rewriteHostHeader = value;
    }

    public void setHttpClientService(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }
}
