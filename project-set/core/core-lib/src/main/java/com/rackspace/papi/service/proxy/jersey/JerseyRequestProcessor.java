package com.rackspace.papi.service.proxy.jersey;

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
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 *
 */
class JerseyRequestProcessor extends AbstractRequestProcessor {

   private static final Logger LOG = LoggerFactory.getLogger(JerseyRequestProcessor.class);
   private final HttpServletRequest request;
   private Pattern delimiter = Pattern.compile("&");
   private Pattern pair = Pattern.compile("=");

   public JerseyRequestProcessor(HttpServletRequest request) throws IOException {
      this.request = request;
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
     * Copy header values from source request to the http method.
     *
     * @param method
     */
   private void setHeaders(PartialRequestBuilder builder) {
      final Enumeration<String> headerNames = request.getHeaderNames();

      while (headerNames.hasMoreElements()) {
         String header = headerNames.nextElement();

         if (!excludeHeader(header)) {
            Enumeration<String> values = request.getHeaders(header);
            while (values.hasMoreElements()) {
               String value = values.nextElement();
               builder.header(header, value);
            }
         }
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

   /**
     * Process an entity enclosing http method. These methods can handle a
     * request body.
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
         builder.entity(input);
      }
     return builder;
   }
}