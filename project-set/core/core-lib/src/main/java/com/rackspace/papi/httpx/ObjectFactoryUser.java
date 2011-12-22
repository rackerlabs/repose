package com.rackspace.papi.httpx;

import com.rackspace.httpx.ObjectFactory;

/**
 * @author fran
 */
public abstract class ObjectFactoryUser {

   private ObjectFactory objectFactory = new ObjectFactory();

   public ObjectFactory getObjectFactory() {
      return objectFactory;
   }

   public void setObjectFactory(ObjectFactory objectFactory) {
      this.objectFactory = objectFactory;
   }
}
