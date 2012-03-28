package com.rackspace.papi.http.proxy.httpclient;

import com.rackspace.papi.http.proxy.common.AbstractResponseProcessor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.http.HttpException;

public class HttpResponseProcessor extends AbstractResponseProcessor {
    private final HttpMethod httpMethodResponse;
    
    public HttpResponseProcessor(HttpMethod httpMethodProxyRequest, HttpServletResponse response, HttpResponseCodeProcessor responseCode) {
       super(response, responseCode.getCode());
      this.httpMethodResponse = httpMethodProxyRequest;
    }
    
   @Override
    protected void setResponseHeaders() throws IOException {
      for (Header header : httpMethodResponse.getResponseHeaders()) {
          addHeader(header.getName(), header.getValue());
      }
    }
    
   @Override
    protected void setResponseBody() throws IOException {
      final InputStream source = httpMethodResponse.getResponseBodyAsStream();
      if (source != null) {

        final BufferedInputStream httpIn = new BufferedInputStream(source);
        final OutputStream clientOut = getResponse().getOutputStream();

        //Using a buffered stream so this isn't nearly as expensive as it looks
        int readData;

        while ((readData = httpIn.read()) != -1) {
            clientOut.write(readData);
        }

        clientOut.flush();
      }
    }
    
   @Override
    protected String getResponseHeaderValue(String headerName) throws HttpException {
        final Header locationHeader = httpMethodResponse.getResponseHeader(headerName);
        if (locationHeader == null) {
            throw new HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + getResponseCode() + ")");
        }

        final String locationValue = locationHeader.getValue();
        if (locationValue == null) {
            throw new HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + getResponseCode() + ")");
        }
        
        return locationValue;
    }
    
}
