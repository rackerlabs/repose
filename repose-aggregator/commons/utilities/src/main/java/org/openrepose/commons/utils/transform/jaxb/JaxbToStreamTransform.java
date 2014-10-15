package org.openrepose.commons.utils.transform.jaxb;

import org.openrepose.commons.utils.pooling.ResourceContextException;
import org.openrepose.commons.utils.pooling.SimpleResourceContext;
import org.openrepose.commons.utils.transform.StreamTransform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;

public class JaxbToStreamTransform<T extends OutputStream> extends AbstractJaxbTransform implements StreamTransform<JAXBElement, T> {

   public JaxbToStreamTransform(JAXBContext ctx) {
      super(ctx);
   }

   @Override
   public void transform(final JAXBElement source, final T target) {
      getMarshallerPool().use(new SimpleResourceContext<Marshaller>() {

         @Override
         public void perform(Marshaller resource) throws ResourceContextException {
            try {
               resource.marshal(source, target);
            } catch (JAXBException jaxbe) {
               throw new ResourceContextException(jaxbe.getMessage(), jaxbe);
            }
         }
      });
   }
}
