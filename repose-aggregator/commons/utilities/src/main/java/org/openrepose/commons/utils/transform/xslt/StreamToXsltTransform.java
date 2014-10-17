package org.openrepose.commons.utils.transform.xslt;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.transform.StreamTransform;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author zinic
 */
public class StreamToXsltTransform extends AbstractXslTransform implements StreamTransform<InputStream, OutputStream> {

   public StreamToXsltTransform(Templates transformationTemplates) {
      super(transformationTemplates);
   }

   @Override
   public void transform(final InputStream source, final OutputStream target) {
        Transformer pooledObject = null;
        final ObjectPool<Transformer> objectPool = getXslTransformerPool();
        try {
            pooledObject = objectPool.borrowObject();
            try {
                pooledObject.transform(new StreamSource(source), new StreamResult(target));
            } catch (TransformerException te) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
               throw new XsltTransformationException("Failed while attempting XSLT transformation.", te);
            } catch (Exception e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                throw new XsltTransformationException("Failed while attempting XSLT transformation.", e);
            } finally {
                if (null != pooledObject) {
                    objectPool.returnObject(pooledObject);
                }
            }
        } catch (XsltTransformationException e) {
            throw e;
        } catch (Exception e) {
            throw new XsltTransformationException("Failed to obtain a Transformer for XSLT transformation.", e);
        }
   }
}
