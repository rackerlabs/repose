package org.openrepose.commons.utils.transform.jaxb;

import org.openrepose.commons.utils.pooling.ResourceConstructionException;
import org.openrepose.commons.utils.pooling.ResourceContext;
import org.openrepose.commons.utils.pooling.ResourceContextException;
import org.openrepose.commons.utils.transform.Transform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

public class JaxbEntityToXml extends AbstractJaxbTransform implements Transform<JAXBElement, String> {

   public JaxbEntityToXml(JAXBContext ctx) {
      super(ctx);
   }

   @Override
   public String transform(final JAXBElement source) {
      return getMarshallerPool().use(new ResourceContext<Marshaller, String>() {

         @Override
         public String perform(Marshaller resource) throws ResourceContextException {
            final StringWriter w = new StringWriter();

            try {
               resource.marshal(source, w);
               return w.getBuffer().toString();
            } catch (JAXBException jaxbe) {
               throw new ResourceConstructionException(jaxbe.getMessage(), jaxbe);
            }
         }
      });
   }
}
