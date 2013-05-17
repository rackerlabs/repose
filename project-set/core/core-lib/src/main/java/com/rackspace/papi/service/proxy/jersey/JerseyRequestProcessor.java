package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.http.proxy.common.AbstractRequestProcessor;
import com.sun.jersey.api.client.PartialRequestBuilder;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process a request to copy over header values, query string parameters, and request body as necessary.
 *
 */
class JerseyRequestProcessor extends AbstractRequestProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(JerseyRequestProcessor.class);
  private static final int BUFFER_SIZE = 1024;
  private final HttpServletRequest request;
  private final URI targetHost;
  private Pattern delimiter = Pattern.compile("&");
  private Pattern pair = Pattern.compile("=");
  private boolean allowBody;
  private final boolean rewriteHostHeader;

  public JerseyRequestProcessor(HttpServletRequest request, URI host, boolean rewriteHostHeader) throws IOException {
    this.request = request;
    this.targetHost = host;
    this.rewriteHostHeader = rewriteHostHeader;
    allowBody = "PUT".equalsIgnoreCase(request.getMethod()) || "POST".equalsIgnoreCase(request.getMethod());
  }

  public WebResource setRequestParameters(WebResource method) {
    WebResource newMethod = method;
    final String queryString = request.getQueryString();

    if (queryString != null && queryString.length() > 0) {
      String[] params = delimiter.split(queryString);

      for (String param : params) {
        String[] paramPair = pair.split(param, 2);
        if (paramPair.length == 2) {
          String paramValue = paramPair[1];
          try {
            paramValue = URLDecoder.decode(paramValue, "UTF-8");
          } catch (IllegalArgumentException ex) {
            LOG.warn("Error decoding query parameter named: " + paramPair[0] + " value: " + paramValue, ex);
          } catch (UnsupportedEncodingException ex) {
            LOG.warn("Error decoding query parameter named: " + paramPair[0] + " value: " + paramValue, ex);
          }
          newMethod = newMethod.queryParam(paramPair[0], paramValue);
        } else {
          newMethod = newMethod.queryParam(paramPair[0], "");
        }
      }
    }

    return newMethod;
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
   */
  private void setHeaders(PartialRequestBuilder builder) {
    final Enumeration<String> headerNames = request.getHeaderNames();

    boolean hasAccept=false;
    while (headerNames.hasMoreElements()) {
      String header = headerNames.nextElement();
      if(StringUtilities.nullSafeEqualsIgnoreCase(header, CommonHttpHeader.ACCEPT.name())){
         hasAccept=true;
      }
      if (!excludeHeader(header)) {
        Enumeration<String> values = request.getHeaders(header);
        //Need to catch that they passed in an empty accept header
        if(StringUtilities.nullSafeEqualsIgnoreCase(header, CommonHttpHeader.ACCEPT.name()) && !values.hasMoreElements()){
           hasAccept=false;
        }
        while (values.hasMoreElements()) {
          String value = values.nextElement();
          builder.header(header, processHeaderValue(header, value));
        }
      }
    }
    
    //Jersey Client will fill the accept header with 'text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2' if none is provided
    //This logic will cause an empty accept header to be sent instead.
    if(!hasAccept){
       builder.accept("");
    }
  }

  private InputStream getRequestStream() throws IOException {
    InputStream in = request.getInputStream();

    if (in == null) {
      return null;
    }
    PushbackInputStream stream = new PushbackInputStream(in, 1);

    int read = stream.read();
    if (read == -1) {
      return null;
    }

    stream.unread(read);
    return stream;
  }

  private void consumeInput(InputStream input) throws IOException {
    if (input == null) {
      return;
    }

    byte[] buffer = new byte[BUFFER_SIZE];

    int read = input.read(buffer);

    while (read > 0) {
      read = input.read(buffer);
    }

    input.close();
  }

  /**
   * Process an entity enclosing http method. These methods can handle a request body.
   *
   * @param method
   * @return
   * @throws IOException
   */
  public Builder process(WebResource method) throws IOException {
    return process(setRequestParameters(method).getRequestBuilder());
  }

  public <T extends PartialRequestBuilder> T process(T builder) throws IOException {

    setHeaders(builder);
    InputStream input = getRequestStream();
    if (input != null) {
      if (allowBody) {
        builder.entity(input);
      } else {
        consumeInput(input);
      }
    }
    return builder;
  }
}