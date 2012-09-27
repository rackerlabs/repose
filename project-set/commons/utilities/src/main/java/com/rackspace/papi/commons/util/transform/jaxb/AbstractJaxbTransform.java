package com.rackspace.papi.commons.util.transform.jaxb;

import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author zinic
 */
public abstract class AbstractJaxbTransform {

   private final Pool<Marshaller> marshallerPool;
   private final Pool<Unmarshaller> unmarshallerPool;
   private final JAXBContext jaxbContext;

   public AbstractJaxbTransform(JAXBContext ctx) {
      jaxbContext = ctx;

      marshallerPool = new GenericBlockingResourcePool<Marshaller>(new ConstructionStrategy<Marshaller>() {

         @Override
         public Marshaller construct() throws ResourceConstructionException {
            try {
               return jaxbContext.createMarshaller();
            } catch (JAXBException jaxbe) {
               throw new ResourceConstructionException(jaxbe.getMessage(), jaxbe);
            }
         }
      });

      unmarshallerPool = new GenericBlockingResourcePool<Unmarshaller>(new ConstructionStrategy<Unmarshaller>() {

         @Override
         public Unmarshaller construct() throws ResourceConstructionException {
            try {
               return jaxbContext.createUnmarshaller();
            } catch (JAXBException jaxbe) {
               throw new ResourceConstructionException(jaxbe.getMessage(), jaxbe);
            }
         }
      });
   }

   protected Pool<Marshaller> getMarshallerPool() {
      return marshallerPool;
   }

   protected Pool<Unmarshaller> getUnmarshallerPool() {
      return unmarshallerPool;
   }
}
