package org.openrepose.filters.translation.httpx.marshaller;

import org.openrepose.core.httpx.MessageEnvelope;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author fran
 */
public class MessageEnvelopeMarshaller extends ObjectFactoryUser implements org.openrepose.filters.translation.httpx.marshaller.Marshaller<MessageEnvelope> {
    private static final String HTTPX_SCHEMA_LOCATION = "http://docs.rackspace.com/httpx/v1.0 ./httpx.xsd";

    @Override
    public InputStream marshall(MessageEnvelope messageEnvelope) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("org.openrepose.core.httpx");

            javax.xml.bind.Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_SCHEMA_LOCATION, HTTPX_SCHEMA_LOCATION);

            marshaller.marshal(getObjectFactory().createHttpx(messageEnvelope), outputStream);
        } catch (JAXBException e) {
            throw new MarshallerException("An exception occurred when attempting to marshal the http message envelope. Reason: " + e.getMessage(), e);
        }

        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
