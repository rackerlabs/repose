package org.openrepose.commons.config.parser.jaxb.test;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UnmarshallerValidatorTestImpl")
public class UnmarshallerValidatorTestImpl {
    @XmlAttribute(name = "unmarshaller-test-attribute", required = true)
    protected String unmarshallerTestAttribute;
}
