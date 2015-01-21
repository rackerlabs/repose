package org.openrepose.services.httpclient.impl;

import org.apache.http.client.params.CookiePolicy;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.openrepose.core.service.httpclient.config.PoolType;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.UUID;

public final class HttpConnectionPoolProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HttpConnectionPoolProvider.class);
    private static final int DEFAULT_HTTPS_PORT = 443;
    private static final String CHUNKED_ENCODING_PARAM = "chunked-encoding";
    public static final String CLIENT_INSTANCE_ID = "CLIENT_INSTANCE_ID";

    private HttpConnectionPoolProvider() {}

    public static HttpClient genClient(PoolType poolConf) {

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
        params.setBooleanParameter(CHUNKED_ENCODING_PARAM, poolConf.isChunkedEncoding());

        final String uuid =  UUID.randomUUID().toString();
        params.setParameter(CLIENT_INSTANCE_ID, uuid);

        //Pass in the params and the connection manager
        DefaultHttpClient client = new DefaultHttpClient(cm, params);

        SSLContext sslContext = ProxyUtilities.getTrustingSslContext();
        SSLSocketFactory ssf = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = cm.getSchemeRegistry();
        Scheme scheme = new Scheme("https", DEFAULT_HTTPS_PORT, ssf);
        registry.register(scheme);

        client.setKeepAliveStrategy(new ConnectionKeepAliveWithTimeoutStrategy(poolConf.getKeepaliveTimeout()));

        LOG.info("HTTP connection pool {} with instance id {} has been created", poolConf.getId(), uuid);

        return client;
    }
}
