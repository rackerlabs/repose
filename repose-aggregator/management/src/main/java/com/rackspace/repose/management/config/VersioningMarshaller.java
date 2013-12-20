package com.rackspace.repose.management.config;

import com.rackspace.papi.components.versioning.config.ObjectFactory;
import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 30, 2012
 * Time: 2:10:20 PM
 */
public class VersioningMarshaller implements ReposeMarshaller {

    private static final Logger LOG = LoggerFactory.getLogger(VersioningMarshaller.class);

    private final JAXBContext jaxbContext = JAXBContext.newInstance(ReposeConfiguration.VERSIONING.getConfigContextPath());
    private final Marshaller marshaller = jaxbContext.createMarshaller();
    private final ObjectFactory objectFactory = new ObjectFactory();

    public VersioningMarshaller() throws JAXBException {
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    }

    @Override
    public void marshal(String configurationRoot, Object config) throws FileNotFoundException, JAXBException {
        if (!(config instanceof ServiceVersionMappingList)) {
            // TODO: Clean up exception handling
            throw new IllegalArgumentException("The config object passed is not a RateLimitingConfiguration.");
        }

        marshaller.marshal(objectFactory.createVersioning((ServiceVersionMappingList) config),
                new FileOutputStream(configurationRoot + ReposeConfiguration.VERSIONING.getConfigFilename()));

        LOG.info("Created " + ReposeConfiguration.VERSIONING.getConfigFilename() + " : " + config.toString());
    }

    @Override
    public JAXBElement<?> unmarshal(String configurationRoot) throws FileNotFoundException, JAXBException {
        return (JAXBElement<ServiceVersionMappingList>) jaxbContext.createUnmarshaller()
                .unmarshal(new File(configurationRoot + ReposeConfiguration.VERSIONING.getConfigFilename()));

    }
}
