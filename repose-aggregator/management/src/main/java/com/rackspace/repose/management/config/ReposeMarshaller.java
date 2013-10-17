package com.rackspace.repose.management.config;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 30, 2012
 * Time: 1:28:30 PM
 */
public interface ReposeMarshaller {

   void marshal(String configurationRoot, java.lang.Object config) throws FileNotFoundException, JAXBException;
   JAXBElement<?> unmarshal(String configurationRoot) throws FileNotFoundException, JAXBException;
}
