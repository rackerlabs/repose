package org.openrepose.common.auth;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.openrepose.commons.utils.pooling.ResourceConstructionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * @author fran
 */
public class ResponseUnmarshaller {

 private static final Logger LOG = LoggerFactory.getLogger(ResponseUnmarshaller.class);
   private final JAXBContext jaxbContext;
   private final ObjectPool<Unmarshaller> objectPool;

   public ResponseUnmarshaller(JAXBContext jaxbContext) {
      this.jaxbContext = jaxbContext;
      objectPool = new SoftReferenceObjectPool<>(new BasePoolableObjectFactory<Unmarshaller>() {

         @Override
         public Unmarshaller makeObject() {
            try {
               return ResponseUnmarshaller.this.jaxbContext.createUnmarshaller();
            } catch (JAXBException ex) {
               throw new ResourceConstructionException("Unable to build jaxb unmarshaller", ex);
            }
         }
      });
   }

   public <T> T unmarshall(final InputStream data, final Class<T> expectedType) {
        Object rtn = null;
        Unmarshaller pooledObject = null;
        try {
            pooledObject = objectPool.borrowObject();
            try {
                final Object unmarshalledObject = pooledObject.unmarshal(new InputStreamReader(data, "UTF8"));
                if (unmarshalledObject instanceof JAXBElement) {
                    rtn = ((JAXBElement) unmarshalledObject).getValue();
                } else {
                    rtn = unmarshalledObject;
                }
            } catch (UnsupportedEncodingException e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Error reading Response stream in Response Unmarshaller", e);
            } catch (JAXBException jaxbe) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                throw new AuthServiceException("Failed to unmarshall response body. Body output is in debug. Reason: " + jaxbe.getMessage(), jaxbe);
            } catch (Exception e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Error reading Response stream in Response Unmarshaller", e);
            } finally {
                if (null != pooledObject) {
                    objectPool.returnObject(pooledObject);
                }
            }
        } catch (AuthServiceException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error obtaining Response Unmarshaller", e);
        }
        if (!expectedType.isInstance(rtn)) {
            throw new AuthServiceException("Failed to unmarshall response body. Unexpected element encountered. Body output is in debug.");
        }
        return expectedType.cast(rtn);
   }
}
