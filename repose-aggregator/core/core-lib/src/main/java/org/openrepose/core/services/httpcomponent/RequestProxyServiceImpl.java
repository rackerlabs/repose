package org.openrepose.core.services.httpcomponent;

import com.google.common.base.Optional;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUriUtilities;
import org.openrepose.commons.utils.http.HttpStatusCode;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.commons.utils.proxy.ProxyRequestException;
import org.openrepose.commons.utils.proxy.RequestProxyService;
import org.openrepose.core.filter.SystemModelInterrogator;
import org.openrepose.core.proxy.HttpException;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.openrepose.services.httpclient.HttpClientNotFoundException;
import org.openrepose.services.httpclient.HttpClientResponse;
import org.openrepose.services.httpclient.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

@Named
@Lazy
public class RequestProxyServiceImpl implements RequestProxyService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProxyServiceImpl.class);
    public static final String SYSTEM_MODEL_CONFIG_HEALTH_REPORT = "SystemModelConfigError";
    private static final String CHUNKED_ENCODING_PARAM = "chunked-encoding";

    private final ConfigurationService configurationManager;
    private final SystemModelListener systemModelListener;
    private final HealthCheckService healthCheckService;
    private final String clusterId;
    private final String nodeId;

    private boolean rewriteHostHeader = false;
    private HttpClientService httpClientService;
    private HealthCheckServiceProxy healthCheckServiceProxy;

    @Inject
    public RequestProxyServiceImpl(ConfigurationService configurationManager,
                                   HealthCheckService healthCheckService,
                                   @Value(ReposeSpringProperties.NODE.CLUSTER_ID) String clusterId,
                                   @Value(ReposeSpringProperties.NODE.NODE_ID) String nodeId) {

        this.configurationManager = configurationManager;
        this.healthCheckService = healthCheckService;
        this.clusterId = clusterId;
        this.nodeId = nodeId;

        this.systemModelListener = new SystemModelListener();
    }

    @PostConstruct
    public void init() {
        healthCheckServiceProxy = healthCheckService.register();

        configurationManager.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);
    }

    @PreDestroy
    public void destroy() {
        configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
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
            LOG.error("Failed to obtain an HTTP default client connection.");
            throw new ProxyRequestException("Failed to obtain an HTTP default client connection.", e);
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
        } catch (URISyntaxException ex) {
            LOG.error("Error processing request", ex);
        } catch (HttpException ex) {
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

    private class SystemModelListener implements UpdateListener<SystemModel> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(SystemModel config) {
            SystemModelInterrogator systemModelInterrogator = new SystemModelInterrogator(clusterId, nodeId);
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
}
