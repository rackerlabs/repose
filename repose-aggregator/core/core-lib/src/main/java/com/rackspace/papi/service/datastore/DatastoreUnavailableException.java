package com.rackspace.papi.service.datastore;

/**
 *
 * @author zinic
 */
public class DatastoreUnavailableException extends RuntimeException {

   private final DatastoreService serviceReference;

   public DatastoreUnavailableException(DatastoreService serviceReference, String string) {
      super(string);
      this.serviceReference = serviceReference;
   }

   public DatastoreUnavailableException(DatastoreService serviceReference, String string, Throwable thrwbl) {
      super(string, thrwbl);
      this.serviceReference = serviceReference;
   }

   public DatastoreService getDatastoreService() {
      return serviceReference;
   }
}
