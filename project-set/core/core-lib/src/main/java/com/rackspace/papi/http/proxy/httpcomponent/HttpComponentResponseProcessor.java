package com.rackspace.papi.http.proxy.httpcomponent;

import org.apache.http.HttpException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import com.rackspace.papi.http.proxy.common.AbstractResponseProcessor;

public class HttpComponentResponseProcessor extends AbstractResponseProcessor {
    private final HttpResponse httpResponse;
    
    public HttpComponentResponseProcessor(HttpResponse httpResponse, HttpServletResponse response, HttpComponentResponseCodeProcessor responseCode) {
       super(response, responseCode.getCode());
      this.httpResponse = httpResponse;
    }
    
   @Override
    protected void setResponseHeaders() throws IOException {
      for (Header header : httpResponse.getAllHeaders()) {
         addHeader(header.getName(), header.getValue());
      }
    }
    
   @Override
    protected void setResponseBody() throws IOException {
      HttpEntity entity = httpResponse.getEntity();
      if (entity != null) {
        final OutputStream clientOut = getResponse().getOutputStream();
        entity.writeTo(clientOut);
        clientOut.flush();
      }
    }
    
   
   @Override
    protected String getResponseHeaderValue(String headerName) throws HttpException {
        final Header[] locationHeader = httpResponse.getHeaders(headerName);
        if (locationHeader == null || locationHeader.length == 0) {
            throw new HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + getResponseCode() + ")");
        }

        final String locationValue = locationHeader[0].getValue();
        if (locationValue == null) {
            throw new HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + getResponseCode() + ")");
        }
        
        return locationValue;
    }
    
}
