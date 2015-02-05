package org.openrepose.filters.translation.httpx;

import org.openrepose.core.httpx.ObjectFactory;

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
