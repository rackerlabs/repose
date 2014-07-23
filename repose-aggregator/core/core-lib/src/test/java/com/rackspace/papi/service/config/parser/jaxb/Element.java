package com.rackspace.papi.service.config.parser.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "element")
public class Element {
    @XmlElement(name = "hello")
    public String hello;
    
    @XmlElement(name = "goodbye")
    public String goodbye;
}



