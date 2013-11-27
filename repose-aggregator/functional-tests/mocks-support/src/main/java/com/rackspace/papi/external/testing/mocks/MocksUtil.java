package com.rackspace.papi.external.testing.mocks;

import javax.xml.bind.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/*

    Marshalling/Unmarshalling utility for request info
 */
public final class MocksUtil {

    public static final String CONTEXT_PATH = "com.rackspace.papi.external.testing.mocks";

    private MocksUtil(){}

    public static RequestInformation getRequestInfo(InputStream inputStream) throws JAXBException {

        JAXBContext jaxbContext = JAXBContext.newInstance(CONTEXT_PATH);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();


        return ((JAXBElement<RequestInformation>)unmarshaller.unmarshal(inputStream)).getValue();
    }

    public static RequestInformation getRequestInfo(String request) throws JAXBException {
        InputStream is = new ByteArrayInputStream(request.getBytes());
        return getRequestInfo(is);
    }

    public static String getRequestInfoXml(RequestInformation requestInformation) throws JAXBException {

        ObjectFactory factory = new ObjectFactory();
        JAXBContext jaxbContext = JAXBContext.newInstance(CONTEXT_PATH);
        Marshaller marshaller = jaxbContext.createMarshaller();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaller.marshal(factory.createRequestInfo(requestInformation), baos);

        return baos.toString();
    }
}
