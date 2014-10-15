package org.openrepose.commons.utils.transform.xslt;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.transform.Transform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class JaxbXsltToStringTransform implements Transform<JAXBElement, String> {

    private final ObjectPool<Transformer> xsltResourcePool;
    private final Templates transformationTemplates;
    private final JAXBContext jaxbContext;
    private final XsltTransformConstruction construction;

    public JaxbXsltToStringTransform(Templates transformTemplates, JAXBContext jaxbContext) {
        this.construction = new XsltTransformConstruction();
        this.transformationTemplates = transformTemplates;
        this.jaxbContext = jaxbContext;

        xsltResourcePool = construction.generateXsltResourcePool(transformationTemplates);
    }

    @Override
    public String transform(final JAXBElement source) {
        String rtn = null;
        Transformer pooledObject = null;
        try {
            pooledObject = xsltResourcePool.borrowObject();
            try {
                final StringWriter stringWriter = new StringWriter();
                final StreamResult resultWriter = new StreamResult(stringWriter);
                pooledObject.transform(new JAXBSource(jaxbContext, source), resultWriter);
                rtn = stringWriter.getBuffer().toString();
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
        return rtn;
    }
}
