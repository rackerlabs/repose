package org.openrepose.commons.utils.transform.jaxb;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.pooling.ResourceContextException;
import org.openrepose.commons.utils.transform.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

public class StreamToJaxbTransform<T> extends AbstractJaxbTransform implements Transform<InputStream, JAXBElement<T>> {

   private static final Logger LOG = LoggerFactory.getLogger(JaxbEntityToXml.class);

   public StreamToJaxbTransform(JAXBContext jc) {
      super(jc);
   }

   @Override
   public JAXBElement<T> transform(final InputStream source) {
        JAXBElement<T> rtn = null;
        Unmarshaller pooledObject = null;
        final ObjectPool<Unmarshaller> objectPool = getUnmarshallerPool();
        try {
            pooledObject = objectPool.borrowObject();
            try {
                rtn = (JAXBElement<T>) pooledObject.unmarshal(source);
            } catch (JAXBException jbe) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                throw new ResourceContextException(jbe.getMessage(), jbe);
            } catch (Exception e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the Marshaller.", e);
            } finally {
                if (pooledObject != null) {
                    objectPool.returnObject(pooledObject);
                }
            }
        } catch (ResourceContextException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to obtain a Marshaller", e);
        }
        return rtn;
    }
}
