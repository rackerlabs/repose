package org.openrepose.commons.utils.transform.jaxb;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.openrepose.commons.utils.pooling.ResourceConstructionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author zinic
 */
public abstract class AbstractJaxbTransform {

   private final ObjectPool<Marshaller> marshallerPool;
   private final ObjectPool<Unmarshaller> unmarshallerPool;
   private final JAXBContext jaxbContext;

   public AbstractJaxbTransform(JAXBContext ctx) {
      jaxbContext = ctx;

      marshallerPool = new SoftReferenceObjectPool<>(new BasePoolableObjectFactory<Marshaller>() {
         @Override
         public Marshaller makeObject() throws Exception {
            try {
               return jaxbContext.createMarshaller();
            } catch (JAXBException jaxbe) {
               throw new ResourceConstructionException(jaxbe.getMessage(), jaxbe);
            }
         }
      });

      unmarshallerPool = new SoftReferenceObjectPool<>(new BasePoolableObjectFactory<Unmarshaller>() {
         @Override
         public Unmarshaller makeObject() throws Exception {
            try {
               return jaxbContext.createUnmarshaller();
            } catch (JAXBException jaxbe) {
               throw new ResourceConstructionException(jaxbe.getMessage(), jaxbe);
            }
         }
      });
   }

   protected ObjectPool<Marshaller> getMarshallerPool() {
      return marshallerPool;
   }

   protected ObjectPool<Unmarshaller> getUnmarshallerPool() {
      return unmarshallerPool;
   }
}
