package com.rackspace.papi.service.datastore.impl.distributed.common;

import com.rackspace.papi.commons.util.http.HeaderConstant;

public enum DatastoreHeader implements HeaderConstant {

   HOST_KEY("X-PP-Host-Key"),
   REMOTE_BEHAVIOR("X-PP-Datastore-Behavior");
   
   private final String headerKey;

   private DatastoreHeader(String headerKey) {
      this.headerKey = headerKey.toLowerCase();
   }

   @Override
   public String toString() {
      return headerKey;
   }

   @Override
   public boolean matches(String st) {
      return headerKey.equalsIgnoreCase(st);
   }
}
