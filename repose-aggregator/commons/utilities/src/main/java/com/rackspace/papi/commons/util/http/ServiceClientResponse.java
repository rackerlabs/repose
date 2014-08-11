package com.rackspace.papi.commons.util.http;

import org.apache.http.Header;

import java.io.InputStream;

/**
 *
 * @author Dan Daley
 */
public class ServiceClientResponse<E> {

   private final E entity;
   private final InputStream data;
   private final int statusCode;
   private final Header[] headers;

   public ServiceClientResponse(int code, E entity) {
      this.statusCode = code;
      this.headers = null;
      this.data = null;
      this.entity = entity;
   }
   
   public ServiceClientResponse(int code, InputStream data) {
      this.statusCode = code;
      this.headers = null;
      this.data = data;
      this.entity = null;
   }

   public ServiceClientResponse(int code, Header[] headers, InputStream data) {
       this.statusCode = code;
       this.headers = headers;
       this.data = data;
       this.entity = null;
   }

   public InputStream getData() {
      return data;
   }

   public E getEntity() {
      return entity;
   }

   public Header[] getHeaders() { return headers; }

   public int getStatusCode() {
      return statusCode;
   }
}
