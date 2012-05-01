package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.QualityFactorHeaderChooser;
import com.rackspace.papi.commons.util.io.BufferedServletInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 *
 * @author jhopper
 */
public class MutableHttpServletRequest extends HttpServletRequestWrapper {

   public static MutableHttpServletRequest wrap(HttpServletRequest request) {
      return request instanceof MutableHttpServletRequest ? (MutableHttpServletRequest) request : new MutableHttpServletRequest(request);
   }
   private final Map<String, List<String>> headers;
   private final List<RouteDestination> destinations;
   private BufferedServletInputStream inputStream;
   private StringBuffer requestUrl;
   private String requestUri, requestUriQuery;
   
   private MutableHttpServletRequest(HttpServletRequest request) {
      super(request);

      requestUrl = request.getRequestURL();
      requestUri = request.getRequestURI();
      
      requestUriQuery = requestUri;

      headers = new HashMap<String, List<String>>();
      destinations = new ArrayList<RouteDestination>();

      copyHeaders(request);
   }
   
   public void addDestination(String id, String uri, float quality) {
      addDestination(new RouteDestination(id, uri, quality));
   }
   
   public void addDestination(RouteDestination dest) {
      if (dest == null) {
         throw new IllegalArgumentException("Destination cannot be null");
      }
      destinations.add(dest);
   }
   
   public RouteDestination getDestination() {
      if (destinations.isEmpty()) {
         return null;
      }
      
      Collections.sort(destinations);
      return destinations.get(destinations.size() - 1);
   }

   @Override
   public String getQueryString() {
      return requestUriQuery;
   }

   public void setQueryString(String requestUriQuery) {
      this.requestUriQuery = requestUriQuery;
   }
   
   @Override
   public ServletInputStream getInputStream() throws IOException {
      synchronized (this) {
         if (inputStream == null) {
            inputStream = new BufferedServletInputStream(super.getInputStream());
         }
      }
      return inputStream;
   }
   
   private void copyHeaders(HttpServletRequest request) {
      final Enumeration<String> headerNames = request.getHeaderNames();

      while (headerNames != null && headerNames.hasMoreElements()) {
         final String headerName = headerNames.nextElement().toLowerCase();  //Normalize to lowercase

         final Enumeration<String> headerValues = request.getHeaders(headerName);
         final List<String> copiedHeaderValues = new LinkedList<String>();

         while (headerValues.hasMoreElements()) {
            copiedHeaderValues.add(headerValues.nextElement());
         }

         headers.put(headerName, copiedHeaderValues);
      }
   }

   @Override
   public String getRequestURI() {
      return requestUri;
   }

   public void setRequestUri(String requestUri) {
      this.requestUri = requestUri;
   }

   @Override
   public StringBuffer getRequestURL() {
      return requestUrl;
   }

   public void setRequestUrl(StringBuffer requestUrl) {
      this.requestUrl = requestUrl;
   }

   public void addHeader(String name, String value) {
      final String lowerCaseName = name.toLowerCase();

      List<String> headerValues = headers.get(lowerCaseName);

      if (headerValues == null) {
         headerValues = new LinkedList<String>();
      }

      headerValues.add(value);

      headers.put(lowerCaseName, headerValues);
   }

   public void replaceHeader(String name, String value) {
      final List<String> headerValues = new LinkedList<String>();

      headerValues.add(value);

      headers.put(name.toLowerCase(), headerValues);
   }

   public void removeHeader(String name) {
      headers.remove(name.toLowerCase());
   }

   @Override
   public String getHeader(String name) {
      return fromMap(headers, name.toLowerCase());
   }

   @Override
   public Enumeration<String> getHeaderNames() {
      return Collections.enumeration(headers.keySet());
   }

   @Override
   public Enumeration<String> getHeaders(String name) {
      final List<String> headerValues = headers.get(name.toLowerCase());

      return Collections.enumeration(headerValues != null ? headerValues : Collections.EMPTY_SET);
   }
   
   public HeaderValue getPreferredHeader(String name) {
      return getPreferredHeader(name, null);
   }
   
   public HeaderValue getPreferredHeader(String name, HeaderValue defaultValue) {
      List<HeaderValue> values = getPreferredHeaderValues(name, defaultValue);
      
      return !values.isEmpty()? values.get(0): null;
   }

   public List<HeaderValue> getPreferredHeaderValues(String name) {
      return getPreferredHeaderValues(name, null);
   }

   public List<HeaderValue> getPreferredHeaderValues(String name, HeaderValue defaultValue) {
      HeaderFieldParser parser = new HeaderFieldParser(headers.get(name.toLowerCase()));
      List<HeaderValue> headerValues = parser.parse();
      
      QualityFactorHeaderChooser chooser = new QualityFactorHeaderChooser<HeaderValue>();
      List<HeaderValue> values = chooser.choosePreferredHeaderValues(headerValues);
      
      if (values.isEmpty() && defaultValue != null) {
         values.add(defaultValue);
      }
      
      return values;
      
   }

   static String fromMap(Map<String, List<String>> headers, String headerName) {
      final List<String> headerValues = headers.get(headerName);

      return (headerValues != null && headerValues.size() > 0) ? headerValues.get(0) : null;
   }
}
