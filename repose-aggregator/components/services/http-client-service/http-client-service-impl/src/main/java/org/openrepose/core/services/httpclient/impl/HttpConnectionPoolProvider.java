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
package org.openrepose.core.services.httpclient.impl;

import io.opentracing.Tracer;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.openrepose.commons.utils.opentracing.httpclient.ReposeTracingRequestInterceptor;
import org.openrepose.commons.utils.opentracing.httpclient.ReposeTracingResponseInterceptor;
import org.openrepose.core.service.httpclient.config.HeaderType;
import org.openrepose.core.service.httpclient.config.PoolType;
import org.openrepose.core.services.uriredaction.UriRedactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class HttpConnectionPoolProvider {

    public static final String CLIENT_INSTANCE_ID = "CLIENT_INSTANCE_ID";
    private static final Logger LOG = LoggerFactory.getLogger(HttpConnectionPoolProvider.class);
    private static final int DEFAULT_HTTPS_PORT = 443;
    private static final String CHUNKED_ENCODING_PARAM = "chunked-encoding";

    private HttpConnectionPoolProvider() {
    }

    public static HttpClient genClient(String configRoot, PoolType poolConf, Tracer tracer, String reposeVersion, UriRedactionService uriRedactionService) {
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();

        cm.setDefaultMaxPerRoute(poolConf.getHttpConnManagerMaxPerRoute());
        cm.setMaxTotal(poolConf.getHttpConnManagerMaxTotal());

        //Set all the params up front, instead of mutating them? Maybe this matters
        HttpParams params = new BasicHttpParams();
        params.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
        params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, poolConf.getHttpSocketTimeout());
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, poolConf.getHttpConnectionTimeout());
        params.setParameter(CoreConnectionPNames.TCP_NODELAY, poolConf.isHttpTcpNodelay());
        params.setParameter(CoreConnectionPNames.MAX_HEADER_COUNT, poolConf.getHttpConnectionMaxHeaderCount());
        params.setParameter(CoreConnectionPNames.MAX_LINE_LENGTH, poolConf.getHttpConnectionMaxLineLength());
        params.setParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, poolConf.getHttpSocketBufferSize());
        params.setParameter(CHUNKED_ENCODING_PARAM, poolConf.getChunkedEncoding().value());

        if (poolConf.getHeaders() != null) {
            params.setParameter(ClientPNames.DEFAULT_HEADERS, createHeaders(poolConf.getHeaders().getHeader()));
        }

        final String uuid = UUID.randomUUID().toString();
        params.setParameter(CLIENT_INSTANCE_ID, uuid);

        //Pass in the params and the connection manager
        DefaultHttpClient client = new DefaultHttpClient(cm, params);

        // OpenTracing support
        // Note that although we always register these interceptors, the provided Tracer may be a NoopTracer,
        // making nearly all of the work done by these interceptors a no-op.
        client.addRequestInterceptor(new ReposeTracingRequestInterceptor(tracer, reposeVersion, uriRedactionService));
        client.addResponseInterceptor(new ReposeTracingResponseInterceptor());

        SSLContext sslContext = ProxyUtilities.getTrustingSslContext();

        if (poolConf.getKeystoreFilename() != null) {
            sslContext = generateSslContextForKeystore(configRoot, poolConf);
        }

        // The SSLSocketFactory will throw IllegalArgumentException if the sslContext is NULL
        SSLSocketFactory ssf = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = cm.getSchemeRegistry();
        Scheme scheme = new Scheme("https", DEFAULT_HTTPS_PORT, ssf);
        registry.register(scheme);

        client.setKeepAliveStrategy(new ConnectionKeepAliveWithTimeoutStrategy(poolConf.getKeepaliveTimeout()));

        LOG.info("HTTP connection pool {} with instance id {} has been created", poolConf.getId(), uuid);

        return client;
    }

    private static SSLContext generateSslContextForKeystore(String configRoot, PoolType poolConf) {
        SSLContext sslContext;

        try {
            SSLContextBuilder sslContextBuilder = SSLContexts.custom();
            File keystoreFile = new File(poolConf.getKeystoreFilename());
            if (!keystoreFile.isAbsolute()) {
                keystoreFile = new File(configRoot, poolConf.getKeystoreFilename());
            }
            char[] keystorePassword = poolConf.getKeystorePassword() == null ? null : poolConf.getKeystorePassword().toCharArray();
            char[] keyPassword = poolConf.getKeyPassword() == null ? null : poolConf.getKeyPassword().toCharArray();
            sslContextBuilder = sslContextBuilder.loadKeyMaterial(keystoreFile, keystorePassword, keyPassword);

            if (poolConf.getTruststoreFilename() == null) {
                sslContextBuilder = sslContextBuilder.loadTrustMaterial(keystoreFile, keystorePassword);
            } else {
                File truststoreFile = new File(poolConf.getTruststoreFilename());
                if (!truststoreFile.isAbsolute()) {
                    truststoreFile = new File(configRoot, poolConf.getTruststoreFilename());
                }
                char[] truststorePassword = poolConf.getTruststorePassword() == null ? null : poolConf.getTruststorePassword().toCharArray();
                sslContextBuilder = sslContextBuilder.loadTrustMaterial(truststoreFile, truststorePassword);
            }

            sslContext = sslContextBuilder.build();
        } catch (GeneralSecurityException | IOException e) {
            LOG.warn("Failed to properly configure the SSL client for {} due to: {}", poolConf.getId(), e.getLocalizedMessage());
            LOG.trace("", e);
            LOG.info("Failing over to basic Trusting SSL context.");
            sslContext = ProxyUtilities.getTrustingSslContext();
        }

        return sslContext;
    }

    private static Collection<Header> createHeaders(List<HeaderType> configHeaders) {
        Collection<Header> headers = new ArrayList<>();

        for (HeaderType configHeader : configHeaders) {
            headers.add(new BasicHeader(configHeader.getName(), configHeader.getValue()));
        }

        return headers;
    }
}
