package com.rackspace.papi.commons.util.transform.xslt;

import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.pooling.SimpleResourceContext;
import com.rackspace.papi.commons.util.transform.StreamTransform;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamResult;

public class XsltToStreamTransform<T extends OutputStream> implements StreamTransform<JAXBElement, T> {

   private final Pool<Transformer> xsltResourcePool;
   private final Templates transformationTemplates;
   private final JAXBContext jaxbContext;

   public XsltToStreamTransform(Templates transformTemplates, JAXBContext jaxbContext) {
      this.transformationTemplates = transformTemplates;
      this.jaxbContext = jaxbContext;

      xsltResourcePool = new GenericBlockingResourcePool<Transformer>(
              new ConstructionStrategy<Transformer>() {

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

   @Override
   public void transform(final JAXBElement source, final T target) {
      xsltResourcePool.use(new SimpleResourceContext<Transformer>() {

         @Override
         public void perform(Transformer resource) throws ResourceContextException {
            try {
               resource.transform(new JAXBSource(jaxbContext, source), new StreamResult(target));
            } catch (Exception e) {
               throw new XsltTransformationException("Failed while attempting XSLT transformation;. Reason: "
                       + e.getMessage(), e);
            }
         }
      });
   }
}
