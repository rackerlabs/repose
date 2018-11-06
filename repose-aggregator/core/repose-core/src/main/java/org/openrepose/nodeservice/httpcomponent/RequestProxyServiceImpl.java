/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.nodeservice.httpcomponent;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.openrepose.commons.utils.StringUriUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.commons.utils.logging.TracingHeaderHelper;
import org.openrepose.commons.utils.logging.TracingKey;
import org.openrepose.core.services.RequestProxyService;
import org.openrepose.core.services.httpclient.CachingHttpClientContext;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.httpclient.HttpClientServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

@Named
@Lazy
public class RequestProxyServiceImpl implements RequestProxyService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProxyServiceImpl.class);

    private final HttpClientService httpClientService;

    @Inject
    public RequestProxyServiceImpl(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    private void setHeaders(HttpRequestBase base, Map<String, String> headers) {

        final Set<Map.Entry<String, String>> entries = headers.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            base.addHeader(entry.getKey(), entry.getValue());
        }

        //Tack on the tracing ID for requests via the dist datastore
        String traceGUID = MDC.get(TracingKey.TRACING_KEY);
        if (!StringUtils.isEmpty(traceGUID)) {
            Header viaHeader = base.getFirstHeader(CommonHttpHeader.VIA);
            base.addHeader(CommonHttpHeader.TRACE_GUID,
                    TracingHeaderHelper.createTracingHeader(traceGUID, viaHeader != null ? viaHeader.getValue() : null)
            );
        }
    }

    @SuppressWarnings("squid:S2093")
    private ServiceClientResponse execute(HttpRequestBase base, String connPoolId) {
        // I'm not exactly sure why this rule is triggering on this since HttpClientContainer does NOT implement one of the required interfaces.
        // It is also not simply closed, but it is released back to the pool from which it was originally retrieved.
        // So it is safe to suppress warning squid:S2093
        HttpClientServiceClient httpClient = httpClientService.getClient(connPoolId);
        try {
            HttpResponse httpResponse = httpClient.execute(base, CachingHttpClientContext.create().setUseCache(false));
            HttpEntity entity = httpResponse.getEntity();
            int responseCode = httpResponse.getStatusLine().getStatusCode();

            InputStream stream = null;
            if (entity != null) {
                stream = new ByteArrayInputStream(RawInputStreamReader.instance().readFully(entity.getContent()));
                EntityUtils.consume(entity);
            }

            return new ServiceClientResponse(responseCode, stream);
        } catch (IOException ex) {
            LOG.error("Error executing request to {}", base.getURI().toString(), ex);
        }

        return new ServiceClientResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
    }

    @Override
    public ServiceClientResponse get(String uri, Map<String, String> headers, String connPoolId) {
        HttpGet get = new HttpGet(uri);
        setHeaders(get, headers);
        return execute(get, connPoolId);
    }

    @Override
    public ServiceClientResponse get(String baseUri, String extraUri, Map<String, String> headers, String connPoolId) {
        return get(StringUriUtilities.appendPath(baseUri, extraUri), headers, connPoolId);
    }

    @Override
    public ServiceClientResponse delete(String baseUri, String extraUri, Map<String, String> headers, String connPoolId) {
        HttpDelete delete = new HttpDelete(StringUriUtilities.appendPath(baseUri, extraUri));
        setHeaders(delete, headers);
        return execute(delete, connPoolId);
    }

    @Override
    public ServiceClientResponse put(String uri, Map<String, String> headers, byte[] body, String connPoolId) {
        HttpPut put = new HttpPut(uri);
        setHeaders(put, headers);
        if (body != null && body.length > 0) {
            put.setEntity(new InputStreamEntity(new ByteArrayInputStream(body), body.length));
        }
        return execute(put, connPoolId);
    }

    @Override
    public ServiceClientResponse put(String baseUri, String path, Map<String, String> headers, byte[] body, String connPoolId) {
        return put(StringUriUtilities.appendPath(baseUri, path), headers, body, connPoolId);
    }

    @Override
    public ServiceClientResponse patch(String baseUri, String path, Map<String, String> headers, byte[] body, String connPoolId) {
        HttpPatch patch = new HttpPatch(StringUriUtilities.appendPath(baseUri, path));
        setHeaders(patch, headers);
        if (body != null && body.length > 0) {
            patch.setEntity(new InputStreamEntity(new ByteArrayInputStream(body), body.length));
        }
        return execute(patch, connPoolId);
    }
}
