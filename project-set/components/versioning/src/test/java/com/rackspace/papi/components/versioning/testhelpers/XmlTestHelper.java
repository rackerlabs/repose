package com.rackspace.papi.components.versioning.testhelpers;

import com.rackspace.papi.components.versioning.VersioningFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 7/19/11
 * Time: 12:51 PM
 */
public class XmlTestHelper {
    private static final Logger LOG = LoggerFactory.getLogger(XmlTestHelper.class);
    public static final SchemaFactory SCHEMA_FACTORY = SchemaFactory.newInstance( "http://www.w3.org/XML/XMLSchema/v1.1" );

    public static Schema getVersioningSchemaInfo() {
        Schema schema = null;

        try {
            schema = SCHEMA_FACTORY.newSchema(
                    new StreamSource[]{
                            new StreamSource(VersioningFilter.class.getResourceAsStream("/META-INF/schema/xml/xml.xsd")),
                            new StreamSource(VersioningFilter.class.getResourceAsStream("/META-INF/schema/atom/atom.xsd")),
                            new StreamSource(VersioningFilter.class.getResourceAsStream("/META-INF/schema/versioning/versioning.xsd"))
                    });
        } catch(SAXException e) {
            LOG.error("Failed to create schema object!", e);
        }

        return schema;
    }

    public static String getXmlString(JAXBElement versioningInfo, Boolean formatXml, Schema schema) throws JAXBException {
        String packageName = versioningInfo.getValue().getClass().getPackage().getName();
        JAXBContext context = JAXBContext.newInstance(packageName);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatXml);

        if (schema != null) {
            marshaller.setSchema(schema);
        }

        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        marshaller.marshal(versioningInfo, oStream);

        return oStream.toString();
    }
}
