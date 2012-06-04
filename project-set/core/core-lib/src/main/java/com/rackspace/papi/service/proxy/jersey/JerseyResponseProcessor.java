package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.http.proxy.HttpException;
import com.rackspace.papi.http.proxy.common.AbstractResponseProcessor;
import com.sun.jersey.api.client.ClientResponse;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

class JerseyResponseProcessor extends AbstractResponseProcessor {

   private final ClientResponse clientResponse;

   public JerseyResponseProcessor(ClientResponse clientResponse, HttpServletResponse response) {
      super(response, clientResponse.getStatus());
      this.clientResponse = clientResponse;
   }
   
   @Override
   protected void setResponseHeaders() throws IOException {
      MultivaluedMap<String, String> headers = clientResponse.getHeaders();
      for (String headerName : headers.keySet()) {
         for (String value : headers.get(headerName)) {
            addHeader(headerName, value);
         }
      }
   }

   @Override
   protected void setResponseBody() throws IOException {
      final InputStream source = clientResponse.getEntityInputStream();
      final int bufferSize = 1024;

      if (source != null) {

         final BufferedInputStream httpIn = new BufferedInputStream(source);
         final OutputStream clientOut = getResponse().getOutputStream();

         //Using a buffered stream so this isn't nearly as expensive as it looks
         int readData;
         byte bytes[] = new byte[bufferSize];

         while ((readData = httpIn.read(bytes)) != -1) {
            clientOut.write(bytes, 0, readData);
         }

         httpIn.close();
         clientOut.flush();
         clientOut.close();
      }
      clientResponse.close();
   }

   @Override
   protected String getResponseHeaderValue(String headerName) throws HttpException {
      final List<String> locationHeader = clientResponse.getHeaders().get(headerName);
      if (locationHeader == null || locationHeader.isEmpty()) {
         throw new HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + getResponseCode() + ")");
      }

      final String locationValue = locationHeader.get(0);
      if (locationValue == null) {
         throw new HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + getResponseCode() + ")");
      }

      return locationValue;
   }

}
