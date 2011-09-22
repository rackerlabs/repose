package com.rackspace.cloud.valve.http.proxy.httpclient;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;

import static com.rackspace.cloud.valve.http.Headers.*;

/**
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 * 
 */
class HttpRequestProcessor {
    private final HttpServletRequest sourceRequest;
    private final HostConfiguration targetHost;

    public HttpRequestProcessor(HttpServletRequest request, HostConfiguration host) {
      this.sourceRequest = request;
      this.targetHost = host;
    }

    /**
     * Copy request parameters (query string) from source request to the
     * http method.
     * 
     * @param method 
     */
    private void setRequestParameters(HttpMethodBase method) {
      List<NameValuePair> pairs = new ArrayList<NameValuePair>();
      Enumeration<String> names = sourceRequest.getParameterNames();

      while (names.hasMoreElements()) {
        String name = names.nextElement();
        String[] values = sourceRequest.getParameterValues(name);
        for (String value: values) {
          pairs.add(new NameValuePair(name, value));
        }
      }

      if (!pairs.isEmpty()) {
        method.setQueryString(pairs.toArray(new NameValuePair[0]));
      }
    }
    
    /**
     * Scan header values and manipulate as necessary.  Host header, if provided,
     * may need to be updated.
     * 
     * @param headerName
     * @param headerValue
     * @return 
     */
    private String processHeaderValue(String headerName, String headerValue) {
        String result = headerValue;
        
        // In case the proxy host is running multiple virtual servers,
        // rewrite the Host header to ensure that we get content from
        // the correct virtual server
        if (headerName.equalsIgnoreCase(HOST.toString())) {
            result = targetHost.getHost() + ":" + targetHost.getPort();
        }
        
        return result;
    }

    /**
     * Copy header values from source request to the http method.
     * 
     * @param method 
     */
    private void setHeaders(HttpMethod method) {
        final Enumeration<String> headerNames = sourceRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            final Enumeration<String> headerValues = sourceRequest.getHeaders(headerName);

            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                method.setRequestHeader(new Header(headerName, processHeaderValue(headerName, headerValue)));
            }
        }
    }

    /**
     * Process a base http request.  Base http methods will not contain a
     * message body.
     * 
     * @param method
     * @return 
     */
    public HttpMethod process(HttpMethodBase method) {
      setHeaders(method);
      setRequestParameters(method);
      return method;

    }

    /**
     * Process an entity enclosing http method.  These methods can handle
     * a request body.
     * 
     * @param method
     * @return
     * @throws IOException 
     */
    public HttpMethod process(EntityEnclosingMethod method) throws IOException {
      setHeaders(method);
      setRequestParameters(method);
      method.setRequestEntity(new InputStreamRequestEntity(sourceRequest.getInputStream()));
      return method;
    }
}