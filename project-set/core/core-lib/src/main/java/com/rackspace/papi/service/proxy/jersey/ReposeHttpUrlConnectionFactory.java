package com.rackspace.papi.service.proxy.jersey;

import com.sun.jersey.client.urlconnection.HttpURLConnectionFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author fran
 */
public class ReposeHttpUrlConnectionFactory implements HttpURLConnectionFactory {

   @Override
   public HttpURLConnection getHttpURLConnection(URL url) throws IOException {
      return new ReposeHttpUrlConnection(url);
   }
}
