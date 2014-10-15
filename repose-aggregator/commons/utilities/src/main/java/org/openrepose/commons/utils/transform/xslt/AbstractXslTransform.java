package org.openrepose.commons.utils.transform.xslt;

import org.openrepose.commons.utils.pooling.ConstructionStrategy;
import org.openrepose.commons.utils.pooling.GenericBlockingResourcePool;
import org.openrepose.commons.utils.pooling.Pool;
import org.openrepose.commons.utils.pooling.ResourceConstructionException;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

public abstract class AbstractXslTransform {

   private final Pool<Transformer> xsltResourcePool;
   private final Templates transformationTemplates;

   public AbstractXslTransform(Templates transformTemplates) {
      this.transformationTemplates = transformTemplates;

      xsltResourcePool = new GenericBlockingResourcePool<Transformer>(new ConstructionStrategy<Transformer>() {

         @Override
         public Transformer construct() throws ResourceConstructionException {
            try {
               return transformationTemplates.newTransformer();
            } catch (TransformerConfigurationException configurationException) {
               throw new XsltTransformationException("Failed to generate XSLT transformer. Reason: "
                       + configurationException.getMessage(), configurationException);
            }
         }
      });
   }

   protected Pool<Transformer> getXslTransformerPool() {
      return xsltResourcePool;
   }
}
