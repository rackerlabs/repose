package com.rackspace.papi.service.rms;


import com.rackspace.papi.commons.util.io.FilePathReaderImpl;
import com.rackspace.papi.service.rms.config.ObjectFactory;
import com.rackspace.papi.service.rms.config.ResponseMessagingConfiguration;
import com.sun.tools.example.debug.event.ThreadDeathEventSet;
import org.apache.xerces.util.DefaultErrorHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class SchemaTest {

    public static class TestResponseMessagingConfig {

        private Validator validator;
        private Unmarshaller jaxbUnmarshaller;
        DocumentBuilderFactory dbf;


        @Before
        public void standUp() throws Exception {

            SchemaFactory sf = SchemaFactory.newInstance( "http://www.w3.org/XML/XMLSchema/v1.1" );
            sf.setFeature( "http://apache.org/xml/features/validation/cta-full-xpath-checking", true );

            Schema schema = sf.newSchema(
                  new StreamSource[]{
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/response-messaging/response-messaging.xsd")),
                  });

            validator = schema.newValidator();

            jaxbUnmarshaller = JAXBContext.newInstance(
                  ResponseMessagingConfiguration.class.getPackage().getName() ).createUnmarshaller();


        }

        /*
        @Test
        public void shouldValidate() throws IOException, SAXException {

            FilePathReaderImpl fileReader = new FilePathReaderImpl( "/META-INF/service/rms/response-messaging2.cfg.xml" );

            validator.validate( new StreamSource( fileReader.getResourceAsStream() ) );
        }

        @Test
        public void shouldUnmarshall() throws SAXException, JAXBException, ParserConfigurationException, IOException {

            FilePathReaderImpl fileReader = new FilePathReaderImpl( "/META-INF/service/rms/response-messaging2.cfg.xml" );

            Document doc = dbf.newDocumentBuilder().parse( new InputSource( fileReader.getResourceAsStream() ) );

            assertNotNull(  doc.getDocumentElement() );

            ResponseMessagingConfiguration config = jaxbUnmarshaller.unmarshal( doc.getDocumentElement(), ResponseMessagingConfiguration.class).getValue();

            assertNotNull( "Expected element should not be null",
                           config.getStatusCode() );
        }
                    */

        /**
         * Here we validate XML against a Schema object, error as expected.
         *
         */
        @Test( expected = SAXParseException.class )
        public void shouldNotValidateDueToAssert() throws IOException, SAXException {

            FilePathReaderImpl fileReader = new FilePathReaderImpl( "/META-INF/service/rms/response-messaging1.cfg.xml" );

            validator.validate( new StreamSource( fileReader.getResourceAsStream() ) );
        }

        /**
         * Here we attempt to configure a DocumentBuilderFactory to use Schema 1.1 & our schema to validate the XML
         * when we parse & create a Document object.
         *
         * We expect an SAXParseException, but am getting:
         *
         * java.lang.IllegalArgumentException: jaxp-order-not-supported: Property
         * 'http://java.sun.com/xml/jaxp/properties/schemaLanguage' must be set before setting property
         * 'http://java.sun.com/xml/jaxp/properties/schemaSource'.
         *
         * which doesn't make sense since I am setting that variable.  Is the 1.1-related value not valid & ignored by
         * the DocumentBuilderFactory?
         */
        @Test
        public void shouldNotUnmarshall1() throws SAXException, JAXBException, ParserConfigurationException, IOException {

            FilePathReaderImpl fileReader = new FilePathReaderImpl( "/META-INF/service/rms/response-messaging1.cfg.xml" );

            dbf = DocumentBuilderFactory.newInstance();

            dbf.setAttribute( "http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/XML/XMLSchema/v1.1" );
            dbf.setAttribute( "http://java.sun.com/xml/jaxp/properties/schemaSource", SchemaTest.class.getResourceAsStream("/META-INF/schema/response-messaging/response-messaging.xsd") );
            dbf.setValidating( true );

            dbf.setNamespaceAware( true );

            DocumentBuilder db = dbf.newDocumentBuilder();

            db.setErrorHandler( new DefaultHandler() );
            Document doc = db.parse( new InputSource( fileReader.getResourceAsStream() ) );

            assertTrue( null == doc.getDocumentElement() );

            ResponseMessagingConfiguration config = jaxbUnmarshaller.unmarshal( doc.getDocumentElement(), ResponseMessagingConfiguration.class).getValue();

        }

        /**
         * Here's another attempt at configuring the DocumentBuilderFactory with the 1.1 Schema.  Now I'm calling
         * setSchema() to wire in the schema.  I'm not getting any exceptions upon parsing.
         *
         */
        @Test
        public void shouldNotUnmarshall2() throws SAXException, JAXBException, ParserConfigurationException, IOException {

            FilePathReaderImpl fileReader = new FilePathReaderImpl( "/META-INF/service/rms/response-messaging1.cfg.xml" );

            dbf = DocumentBuilderFactory.newInstance();

            SchemaFactory sf = SchemaFactory.newInstance( "http://www.w3.org/XML/XMLSchema/v1.1" );
            sf.setFeature( "http://apache.org/xml/features/validation/cta-full-xpath-checking", true );

            Schema schema = sf.newSchema(
                  new StreamSource[]{
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/response-messaging/response-messaging.xsd")),
                  });

            dbf.setSchema(  schema );
            dbf.setNamespaceAware( true );

            DocumentBuilder db = dbf.newDocumentBuilder();

            db.setErrorHandler( new DefaultHandler() );
            Document doc = db.parse( new InputSource( fileReader.getResourceAsStream() ) );

            Element element = doc.getDocumentElement();

            assertTrue( null == element );

            ResponseMessagingConfiguration config = jaxbUnmarshaller.unmarshal( doc.getDocumentElement(), ResponseMessagingConfiguration.class).getValue();

        }

        /*
         * If I'm unable to get DocumentBuilder to validate a Schema 1.1 xml file, we could manually run validate()
         * and not create a DOM if validate throws an error.
         *
         * I think this is fine, but it would be nice to get DocumentBuilder to work, if its possible.
         *
         */
        @Test( expected = SAXParseException.class )
        public void shouldNotUnmarshall3() throws SAXException, JAXBException, ParserConfigurationException, IOException {

            FilePathReaderImpl fileReader = new FilePathReaderImpl( "/META-INF/service/rms/response-messaging1.cfg.xml" );

            dbf = DocumentBuilderFactory.newInstance();

            SchemaFactory sf = SchemaFactory.newInstance( "http://www.w3.org/XML/XMLSchema/v1.1" );
            sf.setFeature( "http://apache.org/xml/features/validation/cta-full-xpath-checking", true );

            Schema schema = sf.newSchema(
                  new StreamSource[]{
                        new StreamSource(SchemaTest.class.getResourceAsStream("/META-INF/schema/response-messaging/response-messaging.xsd")),
                  });

            schema.newValidator().validate(  new StreamSource( fileReader.getResourceAsStream() ) );


            DocumentBuilder db = dbf.newDocumentBuilder();

            db.setErrorHandler( new DefaultHandler() );
            Document doc = db.parse( new InputSource( fileReader.getResourceAsStream() ) );

            ResponseMessagingConfiguration config = jaxbUnmarshaller.unmarshal( doc.getDocumentElement(), ResponseMessagingConfiguration.class).getValue();

        }
    }
}