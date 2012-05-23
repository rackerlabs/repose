package com.rackspace.papi.http.proxy.httpcomponent;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;

import static com.rackspace.papi.http.Headers.HOST;

/**
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 * 
 */
class HttpComponentRequestProcessor {
    private final HttpServletRequest sourceRequest;
    private final HttpHost targetHost;

    public HttpComponentRequestProcessor(HttpServletRequest request, HttpHost host) {
      this.sourceRequest = request;
      this.targetHost = host;
    }

    /**
     * Copy request parameters (query string) from source request to the
     * http method.
     * 
     * @param method 
     */
    private void setRequestParameters(HttpRequestBase method) {
      Enumeration<String> names = sourceRequest.getParameterNames();

      while (names.hasMoreElements()) {
        String name = names.nextElement();
        String[] values = sourceRequest.getParameterValues(name);
        for (String value: values) {
          method.getParams().setParameter(value, value);
        }
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
            result = targetHost.getHostName() + ":" + targetHost.getPort();
        }
        
        return result;
    }

    /**
     * Copy header values from source request to the http method.
     * 
     * @param method 
     */
    private void setHeaders(HttpRequestBase method) {
        final Enumeration<String> headerNames = sourceRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            if ("Content-Length".equalsIgnoreCase(headerName)) {
              continue;
            }

            final Enumeration<String> headerValues = sourceRequest.getHeaders(headerName);

            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                method.addHeader(headerName, processHeaderValue(headerName, headerValue));
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
    public HttpRequestBase process(HttpRequestBase method) {
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
    public HttpRequestBase process(HttpEntityEnclosingRequestBase method) throws IOException {
      setHeaders(method);
      setRequestParameters(method);
      method.setEntity(new InputStreamEntity(sourceRequest.getInputStream(), sourceRequest.getContentLength()));
      return method;
    }
}