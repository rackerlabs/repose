package com.rackspace.papi.components.datastore.impl.distributed;

import org.openrepose.commons.utils.http.HeaderConstant;

public enum DatastoreHeader implements HeaderConstant {

   HOST_KEY("X-PP-Host-Key"),
   REMOTE_BEHAVIOR("X-PP-Datastore-Behavior");
   
   private final String headerKey;

   private DatastoreHeader(String headerKey) {
      this.headerKey = headerKey;
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
