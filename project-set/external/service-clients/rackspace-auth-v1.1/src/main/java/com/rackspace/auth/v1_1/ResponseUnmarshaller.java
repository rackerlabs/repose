package com.rackspace.auth.v1_1;

import com.rackspacecloud.docs.auth.api.v1.FullToken;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;

/**
 * @author fran
 */
public class ResponseUnmarshaller {
    private final Unmarshaller jaxbTypeUnmarshaller;

    public ResponseUnmarshaller() {
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);
            jaxbTypeUnmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException jaxbe) {
            throw new AuthServiceException(
                    "Possible deployment problem! Unable to build JAXB Context for Auth v1.1 schema types. Reason: "
                    + jaxbe.getMessage(), jaxbe);
        }
    }

    public <T> T unmarshall(GetMethod method, Class<T> expectedType) {
        try {
            final Object o = jaxbTypeUnmarshaller.unmarshal(new StringReader(method.getResponseBodyAsString()));

            if (o instanceof JAXBElement && ((JAXBElement) o).getDeclaredType().equals(expectedType)) {
                return ((JAXBElement<T>) o).getValue();
            } else if (o instanceof FullToken) {
                return expectedType.cast(o);
            } else {
                throw new AuthServiceException("Failed to unmarshall response body. Unexpected element encountered. Body output is in debug.");

            }
        } catch (IOException ioe) {
            throw new AuthServiceException("Failed to get response body from response.", ioe);
        } catch (JAXBException jaxbe) {
            throw new AuthServiceException("Failed to unmarshall response body. Body output is in debug. Reason: "
                    + jaxbe.getMessage(), jaxbe);
        }
    }
}
