package com.rackspace.papi.commons.util.transform.xslt;

import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.transform.Transform;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamResult;

public class JaxbXsltToStringTransform implements Transform<JAXBElement, String> {

   private final Pool<Transformer> xsltResourcePool;
   private final Templates transformationTemplates;
   private final JAXBContext jaxbContext;

   public JaxbXsltToStringTransform(Templates transformTemplates, JAXBContext jaxbContext) {
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
   public String transform(final JAXBElement source) {
      return xsltResourcePool.use(new ResourceContext<Transformer, String>() {

         @Override
         public String perform(Transformer resource) throws ResourceContextException {
            final StringWriter stringWriter = new StringWriter();
            final StreamResult resultWriter = new StreamResult(stringWriter);

            try {
               resource.transform(new JAXBSource(jaxbContext, source), resultWriter);
            } catch (Exception e) {
               throw new XsltTransformationException("Failed while attempting XSLT transformation;. Reason: "
                       + e.getMessage(), e);
            }

            return stringWriter.getBuffer().toString();
         }
      });
   }
}
