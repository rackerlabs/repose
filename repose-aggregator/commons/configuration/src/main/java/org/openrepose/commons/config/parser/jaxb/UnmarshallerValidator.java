/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.config.parser.jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.IOException;
import java.io.InputStream;

/**
 * This object executes Schema 1.1 validation (if schema provided) & JAXB unmarshalling for a JAXBContext & XML Schema.
 * <p/>
 * This object was created because JAXB unmarshalling was throwing invalid errors when initialized with certain 1.1
 * schemas.
 */
public class UnmarshallerValidator {

    //The transformer we'll use for translating our configuration xmls
    public static final String XALAN_FACTORY_NAME = "org.apache.xalan.processor.TransformerFactoryImpl";
    private static final Logger LOG = LoggerFactory.getLogger(UnmarshallerValidator.class);
    private Schema schema;
    private Unmarshaller unmarshaller;
    private DocumentBuilder db;

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
     * @return The translated document to be unmarshalled instead!
     */
    public DOMSource validate(Document doc) throws IOException, SAXException {
        if (schema != null) {
            try {
                schema.newValidator().validate(new DOMSource(doc));
                //If we can't parse it, try translating it and validating it again
            } catch (SAXException saxParseException) {
                if (saxParseException.getLocalizedMessage().contains("Cannot find the declaration of element")) {
                    //Run a quick XSLT
                    try {
                        //Tightly coupling to xalan, because saxon brings a handful of problems I don't want to deal with
                        // If HE only is installed, and a license file is available, it generates EE specific code somehow
                        // If EE is installed, and we load up repose in a container, it *always* looks for the license
                        // Xalan doesn't care.
                        new org.apache.xalan.processor.TransformerFactoryImpl();
                        TransformerFactory factory = TransformerFactory.newInstance(XALAN_FACTORY_NAME, this.getClass().getClassLoader());

                        StreamSource styleSource = new StreamSource(this.getClass().getResourceAsStream("/configurationXSLT/consistentNamespace.xsl"));
                        Transformer transformer = factory.newTransformer(styleSource);

                        DOMResult result = new DOMResult();
                        transformer.transform(new DOMSource(doc), result);

                        LOG.warn("DEPRECATION WARNING: One of your config files contains an old namespace, update/check your configs!");

                        DOMSource translatedSource = new DOMSource(result.getNode());

                        schema.newValidator().validate(translatedSource);

                        return translatedSource;
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
        //If we made it to here, that means we didn't need to translate it, or we didn't have a schema.
        // For the other execution path, we'll either throw an exception or return the new document
        return new DOMSource(doc);
    }


    public Object validateUnmarshal(InputStream inputstream) throws JAXBException, IOException, SAXException {

        Document doc;

        try {
            doc = db.parse(inputstream);
        } finally {
            db.reset();
        }
        //We will either get back a DOMSource for the same document, or a new
        DOMSource source = validate(doc);
        return unmarshaller.unmarshal(source);

    }
}
