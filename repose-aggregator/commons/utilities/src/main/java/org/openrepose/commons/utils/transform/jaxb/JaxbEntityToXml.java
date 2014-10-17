package org.openrepose.commons.utils.transform.jaxb;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.pooling.ResourceConstructionException;
import org.openrepose.commons.utils.transform.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

public class JaxbEntityToXml extends AbstractJaxbTransform implements Transform<JAXBElement, String> {

   private static final Logger LOG = LoggerFactory.getLogger(JaxbEntityToXml.class);

   public JaxbEntityToXml(JAXBContext ctx) {
      super(ctx);
   }

   @Override
   public String transform(final JAXBElement source) {
        String rtn = null;
        Marshaller pooledObject = null;
        final ObjectPool<Marshaller> objectPool = getMarshallerPool();
        try {
            pooledObject = objectPool.borrowObject();
            try {
                final StringWriter w = new StringWriter();
                pooledObject.marshal(source, w);
                rtn = w.getBuffer().toString();
            } catch (JAXBException jaxbe) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                throw new ResourceConstructionException(jaxbe.getMessage(), jaxbe);
            } catch (Exception e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the Marshaller.", e);
            } finally {
                if (null != pooledObject) {
                    objectPool.returnObject(pooledObject);
                }
            }
        } catch (ResourceConstructionException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to obtain a Marshaller", e);
        }
        return rtn;
   }
}
