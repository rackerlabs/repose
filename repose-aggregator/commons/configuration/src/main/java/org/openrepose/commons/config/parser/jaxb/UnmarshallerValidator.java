package org.openrepose.commons.config.parser.jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This object executes Schema 1.1 validation (if schema provided) & JAXB unmarshalling for a JAXBContext & XML Schema.
 * <p/>
 * This object was created because JAXB unmarshalling was throwing invalid errors when initialized with certain 1.1
 * schemas.
 */
public class UnmarshallerValidator {

    private static final Logger LOG = LoggerFactory.getLogger(UnmarshallerValidator.class);
    private Schema schema;
    private Unmarshaller unmarshaller;
    private DocumentBuilder db;
    //The transformer we'll use for translating our configuration xmls
    public static final String SAXON_HE_FACTORY_NAME = "net.sf.saxon.TransformerFactoryImpl";

    public UnmarshallerValidator(JAXBContext context) throws JAXBException, ParserConfigurationException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        db = dbf.newDocumentBuilder();

        unmarshaller = context.createUnmarshaller();
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    /**
     * Had to expose this for testing :(
     * The actual solution is to create, either a validator factory for the schemas, or something else that is
     * focused around validation so we can test validation separate from the marshalling.
     * <p/>
     * TODO: refactor this configuration garbage to be less bad.
     *
     * @param doc
     * @return
     */
    public void validate(Document doc) throws IOException, SAXException {
        if (schema != null) {
            try {
                schema.newValidator().validate(new DOMSource(doc));
                //If we can't parse it, try translating it and validating it again
            } catch (SAXException saxParseException) {
                if (saxParseException.getLocalizedMessage().contains("Cannot find the declaration of element")) {
                    //Run a quick XSLT
                    try {
                        //This factory creation is hella slow....
                        TransformerFactory factory = TransformerFactory.newInstance(SAXON_HE_FACTORY_NAME, this.getClass().getClassLoader());

                        StreamSource styleSource = new StreamSource(this.getClass().getResourceAsStream("/configurationXSLT/consistentNamespace.xsl"));
                        Transformer transformer = factory.newTransformer(styleSource);

                        DOMResult result = new DOMResult();
                        transformer.transform(new DOMSource(doc), result);

                        LOG.warn("DEPRECATION WARNING: One of your config files contains an old namespace, update/check your configs!");

                        schema.newValidator().validate(new DOMSource(result.getNode()));
                    } catch (TransformerConfigurationException e) {
                        throw new SAXException("Problem configuring transformer to attempt to translate", e);
                    } catch (TransformerException e) {
                        throw new SAXException("Unable to transform xml", e);
                    }
                } else {
                    throw saxParseException;
                }
            }
        } else {
            LOG.debug("Validate method called, but not given any schema");
        }
    }


    public Object validateUnmarshal(InputStream inputstream) throws JAXBException, IOException, SAXException {

        Document doc;

        try {
            doc = db.parse(inputstream);
        } finally {
            db.reset();
        }
        //This method will either silently succeed, or we'll barf validation exceptions
        validate(doc);
        return unmarshaller.unmarshal(doc);

    }
}
