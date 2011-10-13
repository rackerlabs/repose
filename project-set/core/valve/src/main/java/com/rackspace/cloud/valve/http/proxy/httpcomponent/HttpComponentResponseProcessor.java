package com.rackspace.cloud.valve.http.proxy.httpcomponent;

import org.apache.http.HttpException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import com.rackspace.papi.commons.util.StringUtilities;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import static com.rackspace.cloud.valve.http.Headers.*;

public class HttpComponentResponseProcessor {
    private final HttpResponse httpResponse;
    private final HttpServletResponse response;
    private final HttpComponentResponseCodeProcessor responseCode;
    
    public HttpComponentResponseProcessor(HttpResponse httpResponse, HttpServletResponse response, HttpComponentResponseCodeProcessor responseCode) {
      this.httpResponse = httpResponse;
      this.response = response;
      this.responseCode = responseCode;
    }
    
    private void setResponseHeaders() throws IOException {
      for (Header header : httpResponse.getAllHeaders()) {
          response.setHeader(header.getName(), header.getValue());
      }
    }
    
    private void setResponseBody() throws IOException {
      HttpEntity entity = httpResponse.getEntity();
      if (entity != null) {
        final OutputStream clientOut = response.getOutputStream();
        entity.writeTo(clientOut);
        clientOut.flush();
      }
    }
    
    private String getResponseHeaderValue(String headerName) throws HttpException {
        final Header[] locationHeader = httpResponse.getHeaders(headerName);
        if (locationHeader == null || locationHeader.length == 0) {
            throw new HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + responseCode + ")");
        }

        final String locationValue = locationHeader[0].getValue();
        if (locationValue == null) {
            throw new HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + responseCode + ")");
        }
        
        return locationValue;
    }
    
    private String translateRedirectUrl(String proxiedRedirectUrl, String proxiedHostUrl, String requestHostPath) {
        if (StringUtilities.isEmpty(proxiedRedirectUrl)) {
          return requestHostPath;
        }
        return proxiedRedirectUrl.replace(proxiedHostUrl, requestHostPath);
    }
    
    /**
     * 
     * @param proxiedHostUrl - host:port/contextPath to the origin service
     * @param requestHostPath - host:port/contextPath from the client request
     * @throws HttpException
     * @throws IOException 
     */
    public void sendTranslatedRedirect(String proxiedHostUrl, String requestHostPath) throws HttpException, IOException {
        final String proxiedRedirectUrl = getResponseHeaderValue(LOCATION.name());
        final String translatedRedirectUrl = translateRedirectUrl(proxiedRedirectUrl, proxiedHostUrl, requestHostPath);
        
        response.sendRedirect(translatedRedirectUrl);
    }
    
    public void process() throws IOException {
      response.setStatus(responseCode.getCode());
      if (responseCode.isNotModified()) {
          // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
          response.setIntHeader(CONTENT_LENGTH.toString(), 0);
      } else {
        setResponseHeaders();
        setResponseBody();
      }
    }

  
}
