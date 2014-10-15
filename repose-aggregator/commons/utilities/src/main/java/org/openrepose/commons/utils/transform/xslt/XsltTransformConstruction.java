package org.openrepose.commons.utils.transform.xslt;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

public class XsltTransformConstruction {

    public ObjectPool<Transformer> generateXsltResourcePool(final Templates transformationTemplates) {
        return new SoftReferenceObjectPool<>(
                new BasePoolableObjectFactory<Transformer>() {

                    @Override
                    public Transformer makeObject() throws Exception {
                        try {
                            return transformationTemplates.newTransformer();
                        } catch (TransformerConfigurationException configurationException) {
                            throw new XsltTransformationException("Failed to generate XSLT transformer. Reason: " +
                                                                          configurationException.getMessage(),
                                                                  configurationException);
                        }
                    }
                });
    }
}
