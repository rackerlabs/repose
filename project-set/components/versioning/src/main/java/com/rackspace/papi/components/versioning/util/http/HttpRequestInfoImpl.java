package com.rackspace.papi.components.versioning.util.http;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.servlet.RequestMediaRangeInterrogator;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

// NOTE: This does not belong in util - this is a domain object for versioning only
public class HttpRequestInfoImpl implements HttpRequestInfo {

   private static List<MediaType> getMediaRanges(HttpServletRequest request) {
      MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
      List<HeaderValue> preferredAcceptHeader = mutableRequest.getPreferredHeaderValues(CommonHttpHeader.ACCEPT.toString());
      return RequestMediaRangeInterrogator.interrogate(request.getRequestURI(), preferredAcceptHeader);
   }
   private final List<MediaType> acceptMediaRange;
   private final MediaType preferedMediaRange;
   private final String uri;
   private final String url;
   private final String host;
   private final String scheme;

   public HttpRequestInfoImpl(HttpServletRequest request) {
      this(getMediaRanges(request), request.getRequestURI(), request.getRequestURL().toString(), request.getHeader(CommonHttpHeader.HOST.toString()), request.getScheme());
   }

   public HttpRequestInfoImpl(List<MediaType> acceptMediaRange, String uri, String url, String host, String scheme) {
      this.preferedMediaRange = acceptMediaRange.get(0);
      this.acceptMediaRange = acceptMediaRange;
      this.uri = uri;
      this.url = url;
      this.host = host;
      this.scheme = scheme;
   }

   @Override
   public String getUri() {
      return uri;
   }

   @Override
   public String getUrl() {
      return url;
   }

   @Override
   public MediaType getPreferedMediaRange() {
      return preferedMediaRange;
   }

   @Override
   public boolean hasMediaRange(MediaType targetRange) {
      for (MediaType requestedRange : acceptMediaRange) {
         if (requestedRange.equals(targetRange)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String getHost() {
      return host;
   }
   
   @Override
   public String getScheme() {
      return scheme;
   }
}
