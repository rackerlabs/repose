package com.rackspace.papi.service.proxy.httpcomponent;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.http.proxy.common.AbstractRequestProcessor;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Enumeration;

/**
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 */
class HttpComponentRequestProcessor extends AbstractRequestProcessor {

  private static final String ENCODING = "UTF-8";
  private final HttpServletRequest sourceRequest;
  private final URI targetHost;
  private final boolean rewriteHostHeader;

  public HttpComponentRequestProcessor(HttpServletRequest request, URI host, boolean rewriteHostHeader) {
    this.sourceRequest = request;
    this.targetHost = host;
    this.rewriteHostHeader = rewriteHostHeader;
  }

  private void setRequestParameters(URIBuilder builder) throws URISyntaxException {
    Enumeration<String> names = sourceRequest.getParameterNames();

        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String[] values = sourceRequest.getParameterValues(name);

      for (String value : values) {
        try {
          builder.addParameter(name, URLDecoder.decode(value, ENCODING));
        } catch (UnsupportedEncodingException ex) {
          throw new URISyntaxException(value, "Invalid value for query parameter: " + name);
        }
      }
    }
  }

  public URI getUri(String target) throws URISyntaxException {
    URIBuilder builder = new URIBuilder(target);
    setRequestParameters(builder);
    return builder.build();
  }

  /**
   * Scan header values and manipulate as necessary. Host header, if provided, may need to be updated.
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
    if (rewriteHostHeader && headerName.equalsIgnoreCase(CommonHttpHeader.HOST.toString())) {
      result = targetHost.getHost() + ":" + targetHost.getPort();
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

      if (excludeHeader(headerName)) {
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
   * Process a base http request. Base http methods will not contain a message body.
   *
   * @param method
   * @return
   */
  public HttpRequestBase process(HttpRequestBase method) {
    setHeaders(method);
    return method;

  }

  /**
   * Process an entity enclosing http method. These methods can handle a request body.
   *
   * @param method
   * @return
   * @throws IOException
   */
  public HttpRequestBase process(HttpEntityEnclosingRequestBase method) throws IOException {
    setHeaders(method);
    method.setEntity(new InputStreamEntity(sourceRequest.getInputStream(), -1));
    return method;
  }
}
