package com.rackspace.papi.commons.util.http;

import java.io.InputStream;

/**
 *
 * @author Dan Daley
 */
public class ServiceClientResponse<EntityClass> {

   private final EntityClass entity;
   private final InputStream data;
   private final int statusCode;

   public ServiceClientResponse(int code, EntityClass entity) {
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



   public EntityClass getEntity() {
      return entity;
   }
   
   public int getStatusCode() {
      return statusCode;
   }
}
