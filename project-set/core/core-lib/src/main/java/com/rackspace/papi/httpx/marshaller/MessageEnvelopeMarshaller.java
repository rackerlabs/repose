package com.rackspace.papi.httpx.marshaller;

import com.rackspace.httpx.MessageEnvelope;
import com.rackspace.papi.httpx.ObjectFactoryUser;

import javax.xml.bind.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author fran
 */
public class MessageEnvelopeMarshaller extends ObjectFactoryUser implements com.rackspace.papi.httpx.marshaller.Marshaller<MessageEnvelope> {
    final String HTTPX_SCHEMA_LOCATION = "http://docs.rackspace.com/httpx/v1.0 ./httpx.xsd";

    @Override
    public InputStream marshall(MessageEnvelope messageEnvelope) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("com.rackspace.httpx");

            javax.xml.bind.Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_SCHEMA_LOCATION, HTTPX_SCHEMA_LOCATION);

            marshaller.marshal(getObjectFactory().createHttpx(messageEnvelope), outputStream);
        } catch (JAXBException e) {
            throw new MarshallerException("An exception occurred when attempting to marshal the http message envelope.", e);
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
