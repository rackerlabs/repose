package org.openrepose.rackspace.auth_2_0.identity.parsers.xml;

import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.maps.CredentialFactory;
import org.openrepose.rackspace.auth_2_0.identity.parsers.AuthContentParser;
import org.slf4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;


public class AuthenticationRequestParser implements AuthContentParser {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationRequestParser.class);

    private static final String AUTH_2_0_NAMESPACE = "http://docs.openstack.org/identity/api/v2.0";

    private final Unmarshaller unmarshaller;
    private final NamespaceFilter namespaceFilter;

    public AuthenticationRequestParser(Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
        this.namespaceFilter = new NamespaceFilter(AUTH_2_0_NAMESPACE, true);

        XMLReader reader = null;
        try {
            reader = XMLReaderFactory.createXMLReader();
        } catch (SAXException ex) {
            LOG.error("Unable to read message body stream. Reason: " + ex.getMessage(), ex);
        }

        this.namespaceFilter.setParent(reader);
    }

    @Override
    public AuthCredentials parse(InputStream stream) {

        final InputSource inputSource = new InputSource(stream);
        final SAXSource source = new SAXSource(namespaceFilter, inputSource);


        Object myJaxbObject = null;
        try {
            myJaxbObject = unmarshaller.unmarshal(source);
        } catch (JAXBException ex) {
            LOG.error("Unable to read message body stream. Reason: " + ex.getMessage(), ex);
        }

        return CredentialFactory.getCredentials((JAXBElement<?>) myJaxbObject);
    }

    @Override
    public AuthCredentials parse(String content) {
        Charset CHAR_SET = Charset.forName("UTF-8");
        return this.parse(new ByteArrayInputStream(content.getBytes(CHAR_SET)));
    }
}
