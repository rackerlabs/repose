package com.rackspace.papi.commons.util.http;

import java.io.InputStream;

/**
 *
 * @author Dan Daley
 */
public class ServiceClientResponse<E> {

   private final E entity;
   private final InputStream data;
   private final int statusCode;

   public ServiceClientResponse(int code, E entity) {
      this.statusCode = code;
      this.data = null;
      this.entity = entity;
   }
   
   public ServiceClientResponse(int code, InputStream data) {
      this.statusCode = code;
      this.data = data;
      this.entity = null;
   }

   public InputStream getData() {
      return data;
   }

   public E getEntity() {
      return entity;
   }
   
   public int getStatusCode() {
      return statusCode;
   }
}
