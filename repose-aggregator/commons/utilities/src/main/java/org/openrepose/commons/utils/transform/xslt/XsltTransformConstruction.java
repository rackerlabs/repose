package org.openrepose.commons.utils.transform.xslt;

import org.openrepose.commons.utils.pooling.ConstructionStrategy;
import org.openrepose.commons.utils.pooling.GenericBlockingResourcePool;
import org.openrepose.commons.utils.pooling.Pool;
import org.openrepose.commons.utils.pooling.ResourceConstructionException;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

public class XsltTransformConstruction {

    public Pool<Transformer> generateXsltResourcePool(final Templates transformationTemplates) {
        return new GenericBlockingResourcePool<Transformer>(
                new ConstructionStrategy<Transformer>() {

                    @Override
                    public Transformer construct() throws ResourceConstructionException {
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
