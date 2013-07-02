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
 * Created with IntelliJ IDEA.
 * User: rona6028
 * Date: 7/2/13
 * Time: 9:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class UnmarshallerValidator {

    private Validator validator;
    private Unmarshaller unmarshaller;
    private DocumentBuilder db;

    public UnmarshallerValidator( JAXBContext context ) throws JAXBException, ParserConfigurationException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware( true );

        db = dbf.newDocumentBuilder();

        unmarshaller = context.createUnmarshaller();
    }

    public void addValidator( Validator validator ) {

        this.validator = validator;
    }

    public Object validateUnmarshal( InputStream inputstream ) throws JAXBException, IOException, SAXException {

        Document doc = db.parse( inputstream );

        if (validator != null ) {

            validator.validate( new DOMSource( doc ) );
        }

        return unmarshaller.unmarshal( doc );
    }

}
