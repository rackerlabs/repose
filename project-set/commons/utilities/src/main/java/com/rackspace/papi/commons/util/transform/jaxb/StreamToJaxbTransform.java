package com.rackspace.papi.commons.util.transform.jaxb;

import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.transform.Transform;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

public class StreamToJaxbTransform<T> extends AbstractJaxbTransform implements Transform<InputStream, JAXBElement<T>> {

   public StreamToJaxbTransform(JAXBContext jc) {
      super(jc);
   }

   @Override
   public JAXBElement<T> transform(final InputStream source) {
      return getUnmarshallerPool().use(new ResourceContext<Unmarshaller, JAXBElement<T>>() {

         @Override
         public JAXBElement<T> perform(Unmarshaller resource) throws ResourceContextException {
            try {
               return (JAXBElement<T>) resource.unmarshal(source);
            } catch (JAXBException jbe) {
               throw new ResourceContextException(jbe.getMessage(), jbe);
            }
         }
      });
   }
}
