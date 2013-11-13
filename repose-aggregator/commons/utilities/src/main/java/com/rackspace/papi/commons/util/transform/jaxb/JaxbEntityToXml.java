package com.rackspace.papi.commons.util.transform.jaxb;

import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.transform.Transform;

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
