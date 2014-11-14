package org.openrepose.commons.utils.transform.jaxb;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.pooling.ResourceContextException;
import org.openrepose.commons.utils.transform.StreamTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;

public class JaxbToStreamTransform<T extends OutputStream> extends AbstractJaxbTransform implements StreamTransform<JAXBElement, T> {

   private static final Logger LOG = LoggerFactory.getLogger(JaxbToStreamTransform.class);

   public JaxbToStreamTransform(JAXBContext ctx) {
      super(ctx);
   }

   @Override
   public void transform(final JAXBElement source, final T target) {
        Marshaller pooledObject;
        final ObjectPool<Marshaller> objectPool = getMarshallerPool();
        try {
            pooledObject = objectPool.borrowObject();
            try {
                pooledObject.marshal(source, target);
            } catch (JAXBException jaxbe) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                throw new ResourceContextException(jaxbe.getMessage(), jaxbe);
            } catch (Exception e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the Marshaller. Reason: {}", e.getLocalizedMessage());
                LOG.trace("", e);
            } finally {
                if (pooledObject != null) {
                    objectPool.returnObject(pooledObject);
                }
            }
        } catch (ResourceContextException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to obtain a Marshaller. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
        }
    }
}
