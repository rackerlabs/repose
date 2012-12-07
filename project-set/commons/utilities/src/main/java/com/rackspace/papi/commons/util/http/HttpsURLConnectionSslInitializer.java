package com.rackspace.papi.commons.util.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


/**
 * Used to tell HttpsURLConnection which factory to use when creating sockets.
 */
public class HttpsURLConnectionSslInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(HttpsURLConnectionSslInitializer.class);

    public HttpsURLConnectionSslInitializer() {
    }

    public void verifyServerCerts() {
        // Go back to using the default socket factory that verifies certs
        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, null, null);
            SSLContext.setDefault(sc);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            LOG.error("Problem creating SSL context: ", e);
        }
    }

    public void allowAllServerCerts() {
        TrustManager[] trustAllCerts = new TrustManager[]{new TrustingX509TrustManager()};

        // Install the all-trusting trust manager
        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            SSLContext.setDefault(sc);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            LOG.error("Problem creating SSL context: ", e);
        }
    }

    /**
     * Implementation of the X509TrustManager that does not check any certificates.  WARNING: This
     * should only be used in a development environment!!!
     */
    private static class TrustingX509TrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
