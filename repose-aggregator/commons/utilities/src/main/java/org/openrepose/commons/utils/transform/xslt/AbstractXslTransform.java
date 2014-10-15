package org.openrepose.commons.utils.transform.xslt;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

public abstract class AbstractXslTransform {

   private final ObjectPool<Transformer> xsltResourcePool;
   private final Templates transformationTemplates;

   public AbstractXslTransform(Templates transformTemplates) {
      this.transformationTemplates = transformTemplates;

      xsltResourcePool = new SoftReferenceObjectPool<>(new BasePoolableObjectFactory<Transformer>() {

         @Override
         public Transformer makeObject() throws Exception {
            try {
               return transformationTemplates.newTransformer();
            } catch (TransformerConfigurationException configurationException) {
               throw new XsltTransformationException("Failed to generate XSLT transformer. Reason: "
                       + configurationException.getMessage(), configurationException);
            }
         }
      });
   }

   protected ObjectPool<Transformer> getXslTransformerPool() {
      return xsltResourcePool;
   }
}
