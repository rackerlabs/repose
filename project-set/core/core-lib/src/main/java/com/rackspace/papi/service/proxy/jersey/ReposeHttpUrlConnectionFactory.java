package com.rackspace.papi.service.proxy.jersey;

import com.sun.jersey.client.urlconnection.HttpURLConnectionFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author fran
 */
public class ReposeHttpUrlConnectionFactory implements HttpURLConnectionFactory {
   private static final String HTTPS = "https";

   @Override
   public HttpURLConnection getHttpURLConnection(URL url) throws IOException {

      if (HTTPS.equalsIgnoreCase(url.getProtocol())) {
         return new ReposeHttpsUrlConnection(url);
      } else {
         return new ReposeHttpUrlConnection(url);
      }      
   }
}
