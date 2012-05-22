package com.rackspace.papi.commons.util.transform.jaxb;

import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.pooling.SimpleResourceContext;
import com.rackspace.papi.commons.util.transform.StreamTransform;

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
