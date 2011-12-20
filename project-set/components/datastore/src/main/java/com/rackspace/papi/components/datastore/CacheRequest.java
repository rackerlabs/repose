package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.ArrayUtilities;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import java.net.InetSocketAddress;


import javax.servlet.http.HttpServletRequest;

public class CacheRequest {

   public static final String CACHE_URI_PATH = "/powerapi/dist-datastore/objects/";

   private static String getCacheKey(HttpServletRequest request) {
      final String requestUri = request.getRequestURI();

      if (requestUri.startsWith(CACHE_URI_PATH) && requestUri.length() > CACHE_URI_PATH.length()) {
         final String cacheKey = requestUri.substring(CACHE_URI_PATH.length());

         if (StringUtilities.isBlank(cacheKey)) {
            throw new MalformedCacheRequestException("Cache key specified is invalid");
         }

         return cacheKey;
      }

      return null;
   }

   private static String getHostKey(HttpServletRequest request) {
      final String hostKeyHeader = request.getHeader(DatastoreRequestHeaders.DATASTORE_HOST_KEY);

      if (StringUtilities.isBlank(hostKeyHeader)) {
         throw new MalformedCacheRequestException("No host key specified in header "
                 + DatastoreRequestHeaders.DATASTORE_HOST_KEY + " - this is a required header for this operation");
      }

      return hostKeyHeader;
   }

   public static boolean isCacheRequest(HttpServletRequest request) {
      return request.getRequestURI().startsWith(CACHE_URI_PATH);
   }

   public static CacheRequest marshallCacheGetRequest(HttpServletRequest request) throws MalformedCacheRequestException {
      final String cacheKey = getCacheKey(request);

      return new CacheRequest(cacheKey, getHostKey(request), -1, null);
   }

   public static String urlFor(InetSocketAddress remoteEndpoint, String key) {
      return new StringBuilder("http://").append(remoteEndpoint.getAddress().getHostName()).append(":").append(remoteEndpoint.getPort()).append(CACHE_URI_PATH).append(key).toString();
   }

   public static CacheRequest marshallCachePutRequest(HttpServletRequest request) throws MalformedCacheRequestException {
      final String cacheKey = getCacheKey(request);
      final String hostKey = getHostKey(request);

      try {
         final String ttlHeader = request.getHeader(ExtendedHttpHeader.X_TTL.getHeaderKey());
         final int ttlInSeconds = StringUtilities.isBlank(ttlHeader) ? -1 : Integer.parseInt(ttlHeader);

         return new CacheRequest(cacheKey, hostKey, ttlInSeconds, RawInputStreamReader.instance().readFully(request.getInputStream()));
      } catch (NumberFormatException nfe) {
         throw new MalformedCacheRequestException("Cache object TTL 'X-PP-Datastore-TTL' must be a valid integer number", nfe);
      } catch (Exception ex) {
         throw new MalformedCacheRequestException("Unable to parse object stream contents", ex);
      }
   }
   private final String cacheKey, hostKey;
   private final int ttlInSeconds;
   private final byte[] payload;

   public CacheRequest(String cacheKey, String hostKey, int ttlInSeconds, byte[] payload) {
      this.cacheKey = cacheKey;
      this.hostKey = hostKey;
      this.ttlInSeconds = ttlInSeconds;
      this.payload = ArrayUtilities.nullSafeCopy(payload);
   }

   public int getTtlInSeconds() {
      return ttlInSeconds;
   }

   public boolean hasTtlSet() {
      return ttlInSeconds != -1;
   }

   public String getCacheKey() {
      return cacheKey;
   }

   public String getHostKey() {
      return hostKey;
   }

   public byte[] getPayload() {
      return payload;
   }

   public boolean hasPayload() {
      return payload != null;
   }
}
