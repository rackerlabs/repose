package com.rackspace.papi.http.proxy.jerseyclient;

import com.rackspace.papi.http.proxy.httpclient.HttpResponseCodeProcessor;
import org.apache.http.HttpException;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import com.sun.jersey.api.client.ClientResponse;
import com.rackspace.papi.commons.util.StringUtilities;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import static com.rackspace.papi.http.Headers.*;

public class JerseyResponseProcessor {
    private final ClientResponse clientResponse;
    private final HttpServletResponse response;
    private final HttpResponseCodeProcessor responseCode;
    
    public JerseyResponseProcessor(ClientResponse clientResponse, HttpServletResponse response) {
      this.clientResponse = clientResponse;
      this.response = response;
      this.responseCode = new HttpResponseCodeProcessor(clientResponse.getStatus());
    }
    
    private void setResponseHeaders() throws IOException {
      MultivaluedMap<String, String> headers = clientResponse.getHeaders();
      for (String headerName: headers.keySet()) {
        for (String value: headers.get(headerName)) {
          response.setHeader(headerName, value);
        }
      }
    }
    
    private void setResponseBody() throws IOException {
      final InputStream source = clientResponse.getEntityInputStream();
      final int BUFFER_SIZE = 1024;
              
      if (source != null) {

        final BufferedInputStream httpIn = new BufferedInputStream(source);
        final OutputStream clientOut = response.getOutputStream();

        //Using a buffered stream so this isn't nearly as expensive as it looks
        int readData;
        byte bytes[] = new byte[BUFFER_SIZE];

        while ((readData = httpIn.read(bytes)) != -1) {
            clientOut.write(bytes, 0, readData);
        }

        httpIn.close();
        clientOut.flush();
        clientOut.close();
      }
      clientResponse.close();
    }
    
    private String getResponseHeaderValue(String headerName) throws HttpException {
        final List<String> locationHeader = clientResponse.getHeaders().get(headerName);
        if (locationHeader == null || locationHeader.isEmpty()) {
            throw new HttpException("Expected header was not found in response: " + headerName + " (Response Code: " + responseCode + ")");
        }

        final String locationValue = locationHeader.get(0);
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
