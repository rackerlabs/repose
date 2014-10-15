package org.openrepose.commons.utils.transform.xslt;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.transform.StreamTransform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

public class XsltToStreamTransform<T extends OutputStream> implements StreamTransform<JAXBElement, T> {

    private final ObjectPool<Transformer> xsltResourcePool;
    private final JAXBContext jaxbContext;

    public XsltToStreamTransform(Templates transformTemplates, JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
        final XsltTransformConstruction construction = new XsltTransformConstruction();
        xsltResourcePool = construction.generateXsltResourcePool(transformTemplates);
    }

    @Override
    public void transform(final JAXBElement source, final T target) {
        Transformer pooledObject = null;
        try {
            pooledObject = xsltResourcePool.borrowObject();
            try {
                pooledObject.transform(new JAXBSource(jaxbContext, source), new StreamResult(target));
            } catch (Exception e) {
                xsltResourcePool.invalidateObject(pooledObject);
                pooledObject = null;
                throw new XsltTransformationException("Failed while attempting XSLT transformation.", e);
            } finally {
                if (null != pooledObject) {
                    xsltResourcePool.returnObject(pooledObject);
                }
            }
        } catch (XsltTransformationException e) {
            throw e;
        } catch (Exception e) {
            throw new XsltTransformationException("Failed to obtain a Transformer for XSLT transformation.", e);
        }
    }
}
