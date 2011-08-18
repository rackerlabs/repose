package com.rackspace.papi.commons.util.transform.jaxb;

import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.pooling.SimpleResourceContext;
import com.rackspace.papi.commons.util.transform.StreamTransform;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class OutputStreamJaxbTransform <T> extends AbstractJaxbTransform implements StreamTransform<JAXBElement<T>, OutputStream> {

    public OutputStreamJaxbTransform(JAXBContext ctx) {
        super(ctx);
    }

    @Override
    public void transform(final JAXBElement<T> source, final OutputStream target) {
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
