package com.rackspace.papi.commons.util.proxy;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProxyUtilities {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyUtilities.class);
    
    private ProxyUtilities(){
    }
    
    public static class AllTrustingManager implements X509TrustManager {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
        
    }
    
    public static SSLContext getTrustingSslContext() {
        TrustManager[] trustAllCerts = new TrustManager[]{new AllTrustingManager()};

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            return sc;
        } catch (Exception e) {
            LOG.error("Problem creating SSL context: ", e);
        }

        return null;
    }
}
