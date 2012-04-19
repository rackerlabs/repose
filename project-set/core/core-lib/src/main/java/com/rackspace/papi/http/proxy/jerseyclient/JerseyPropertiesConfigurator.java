package com.rackspace.papi.http.proxy.jerseyclient;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class JerseyPropertiesConfigurator {
   private static final Logger LOG = LoggerFactory.getLogger(JerseyPropertiesConfigurator.class);

   private static final Integer DEFAULT_THREADPOOL_SIZE = 20;
   private final Integer connectionTimeout;
   private final Integer readTimeout;
  
   public JerseyPropertiesConfigurator(Integer connectionTimeout, Integer readTimeout) {
      this.connectionTimeout = connectionTimeout;
      this.readTimeout = readTimeout;
   }

   public DefaultClientConfig configure() {
      DefaultClientConfig cc = new DefaultClientConfig();
      cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
      cc.getProperties().put(ClientConfig.PROPERTY_THREADPOOL_SIZE, DEFAULT_THREADPOOL_SIZE);
      cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectionTimeout);
      cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeout);
      
      // TODO Model: we need to make this configurable so that we don't always
      // accept all certs.
      cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, createJerseySslHttpsProperties());     

      return cc;
   }

   private HTTPSProperties createJerseySslHttpsProperties() {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
          public X509Certificate[] getAcceptedIssuers(){return null;}
          public void checkClientTrusted(X509Certificate[] certs, String authType){}
          public void checkServerTrusted(X509Certificate[] certs, String authType){}
      }};

      // Install the all-trusting trust manager
      SSLContext sc = null;
      try {
          sc = SSLContext.getInstance("SSL");
          sc.init(null, trustAllCerts, new SecureRandom());
          HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      } catch (Exception e) {
          LOG.error("Problem creating SSL context: ", e);
      }

      return new HTTPSProperties( new ReposeHostnameVerifier(), sc);
   }

   private class ReposeHostnameVerifier implements HostnameVerifier {
      @Override
      public boolean verify(String hostname, SSLSession sslSession) {
         LOG.info("verifying: " + hostname);
         return true;
      }
   }
}
