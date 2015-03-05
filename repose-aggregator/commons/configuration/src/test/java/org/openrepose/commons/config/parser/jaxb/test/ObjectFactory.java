package org.openrepose.commons.config.parser.jaxb.test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {
    private final static QName _UnmarshallerTest_QNAME = new QName("http://docs.openrepose.org/repose/unmarshaller-test/v0.0", "unmarshaller-test");

    @XmlElementDecl(namespace = "http://docs.openrepose.org/repose/unmarshaller-test/v0.0", name = "unmarshaller-test")
    public JAXBElement<UnmarshallerValidatorTestImpl> createUnmarshallerTest(UnmarshallerValidatorTestImpl value) {
        return new JAXBElement<UnmarshallerValidatorTestImpl>(_UnmarshallerTest_QNAME, UnmarshallerValidatorTestImpl.class, null, value);
    }
}
