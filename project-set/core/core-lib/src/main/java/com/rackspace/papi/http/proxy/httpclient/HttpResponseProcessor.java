package com.rackspace.papi.http.proxy.httpclient;

import com.rackspace.papi.commons.util.StringUtilities;
import org.apache.commons.httpclient.HttpException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import static com.rackspace.papi.http.Headers.*;

public class HttpResponseProcessor {
    private final HttpMethod httpMethodResponse;
    private final HttpServletResponse response;
    private final HttpResponseCodeProcessor responseCode;
    
    public HttpResponseProcessor(HttpMethod httpMethodProxyRequest, HttpServletResponse response, HttpResponseCodeProcessor responseCode) {
      this.httpMethodResponse = httpMethodProxyRequest;
      this.response = response;
      this.responseCode = responseCode;
    }
    
    private void setResponseHeaders() throws IOException {
      for (Header header : httpMethodResponse.getResponseHeaders()) {
          response.setHeader(header.getName(), header.getValue());
      }
    }
    
    private void setResponseBody() throws IOException {
      final InputStream source = httpMethodResponse.getResponseBodyAsStream();
      if (source != null) {

        final BufferedInputStream httpIn = new BufferedInputStream(source);
        final OutputStream clientOut = response.getOutputStream();

        //Using a buffered stream so this isn't nearly as expensive as it looks
        int readData;

        while ((readData = httpIn.read()) != -1) {
            clientOut.write(readData);
        }

        clientOut.flush();
      }
    }
    
    private String getResponseHeaderValue(String headerName) throws HttpException {
        final Header locationHeader = httpMethodResponse.getResponseHeader(headerName);
        if (locationHeader == null) {
            throw new HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + responseCode + ")");
        }

        final String locationValue = locationHeader.getValue();
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
