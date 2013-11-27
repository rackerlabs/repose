package com.rackspace.papi.commons.util.transform.xslt;

import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;

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
