package com.rackspace.papi.service.proxy.jersey;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

import java.security.NoSuchAlgorithmException;
import javax.net.ssl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JerseyPropertiesConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(JerseyPropertiesConfigurator.class);
    private static final Integer DEFAULT_THREADPOOL_SIZE = 20;
    private final Integer connectionTimeout;
    private final Integer readTimeout;
    private final Integer proxyThreadPool;

    public JerseyPropertiesConfigurator(Integer connectionTimeout, Integer readTimeout, Integer proxyThreadPool) {
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.proxyThreadPool = proxyThreadPool;
    }

    public DefaultClientConfig configure() {
        DefaultClientConfig cc = new DefaultClientConfig();
        cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
        cc.getProperties().put(ClientConfig.PROPERTY_THREADPOOL_SIZE, proxyThreadPool != null? proxyThreadPool: DEFAULT_THREADPOOL_SIZE);
        cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectionTimeout);
        cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeout);

        try {
            // Use the default SSLContext since we use Jersey with HttpsURLConnection.  HttpsURLConnection
            // settings are system-wide (not client-wide).  So let the core Repose set these based on a
            // configuration variable and use the default here.
            cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(new ReposeHostnameVerifier(), SSLContext.getDefault()));
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Error when setting Jersey HTTPS properties", e);
        }

        return cc;
    }

    private static class ReposeHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession sslSession) {
            LOG.info("verifying: " + hostname);
            return true;
        }
    }
}
