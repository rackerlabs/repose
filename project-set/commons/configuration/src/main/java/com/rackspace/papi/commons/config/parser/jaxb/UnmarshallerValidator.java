package com.rackspace.papi.commons.config.parser.jaxb;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * This object executes Schema 1.1 validation (if schema provided) & JAXB unmarshalling for a JAXBContext & XML Schema.
 *
 * This object was created because JAXB unmarshalling was throwing invalid errors when initialized with certain 1.1
 * schemas.
 */
public class UnmarshallerValidator {

    private Schema schema;
    private Unmarshaller unmarshaller;
    private DocumentBuilder db;

    public UnmarshallerValidator( JAXBContext context ) throws JAXBException, ParserConfigurationException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware( true );

        db = dbf.newDocumentBuilder();

        unmarshaller = context.createUnmarshaller();
    }

    public void setSchema( Schema schema ) {

        this.schema = schema;
    }

    public Object validateUnmarshal( InputStream inputstream ) throws JAXBException, IOException, SAXException {

        Document doc;

        try {

            doc = db.parse( inputstream );

        } finally {

            db.reset();
        }

        if (schema != null ) {

            schema.newValidator().validate( new DOMSource( doc ) );
        }

        return unmarshaller.unmarshal( doc );
    }

}
