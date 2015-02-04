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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This object executes Schema 1.1 validation (if schema provided) & JAXB unmarshalling for a JAXBContext & XML Schema.
 *
 * This object was created because JAXB unmarshalling was throwing invalid errors when initialized with certain 1.1
 * schemas.
 */
public class UnmarshallerValidator {

    private static final Logger LOG = LoggerFactory.getLogger(UnmarshallerValidator.class);
    private Schema schema;
    private Unmarshaller unmarshaller;
    private DocumentBuilder db;

    private static final String[] OLD_NAMESPACES = {
            "/docs.api.rackspacecloud.com/repose/", // 0
            "/docs.rackspacecloud.com/repose/",     // 1
            "/openrepose.org/repose/",              // 2
            "/openrepose.org/components/",          // 3
            "/openrepose.org/",                     // 4
            "/docs.openrepose.org/"                 // 5
    };
    private static final String HTTP_HDR = "http:/";

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
            try {
                schema.newValidator().validate(new DOMSource(doc));
            } catch (SAXParseException e) {
                if (e.getLocalizedMessage().contains("Cannot find the declaration of element")) {
                    if (doc != null) {
                        Node node = doc.getFirstChild(); // Root Node
                        if (node != null) {
                            NamedNodeMap namedNodeMap = node.getAttributes();
                            if (namedNodeMap != null) {
                                Node attribute = namedNodeMap.getNamedItem("xmlns");
                                if (attribute != null) {
                                    String namespace = attribute.getNodeValue();
                                    if (namespace != null) {
                                        if (namespace.contains(OLD_NAMESPACES[0]) ||
                                                namespace.contains(OLD_NAMESPACES[1]) ||
                                                namespace.contains(OLD_NAMESPACES[2]) ||
                                                namespace.contains(OLD_NAMESPACES[3]) ||
                                                namespace.contains(OLD_NAMESPACES[4]) ||
                                                namespace.contains(OLD_NAMESPACES[5])) {
                                            LOG.warn("Contains old namespace  - {}", namespace);
                                            StringBuilder stringBuilder = new StringBuilder(namespace);

                                            // Convert any old namespace to the new namespace.
                                            UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[0]), HTTP_HDR + OLD_NAMESPACES[5]);
                                            UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[1]), HTTP_HDR + OLD_NAMESPACES[5]);
                                            UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[2]), HTTP_HDR + OLD_NAMESPACES[5]);
                                            UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[3]), HTTP_HDR + OLD_NAMESPACES[5]);
                                            UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[4]), HTTP_HDR + OLD_NAMESPACES[5]);
                                            UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[5] + "repose/"), HTTP_HDR + OLD_NAMESPACES[5]);

                                            // Add the "repose" is at the end.
                                            UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[5]), HTTP_HDR + OLD_NAMESPACES[5] + "repose/");
                                            LOG.warn("The namespace should be - {}", stringBuilder.toString());
                                            LOG.trace("", e);

                                            try {
                                                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                                Transformer transformer = transformerFactory.newTransformer();
                                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                                StreamResult result = new StreamResult(baos);
                                                transformer.transform(new DOMSource(doc), result);
                                                LOG.trace("ByteArrayOutputStream.toString() = \r\n" + baos.toString());
                                                stringBuilder = new StringBuilder(baos.toString());

                                                // Convert all of the old namespaces to the new namespace.
                                                UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[0]), HTTP_HDR + OLD_NAMESPACES[5]);
                                                UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[1]), HTTP_HDR + OLD_NAMESPACES[5]);
                                                UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[2]), HTTP_HDR + OLD_NAMESPACES[5]);
                                                UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[3]), HTTP_HDR + OLD_NAMESPACES[5]);
                                                UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[4]), HTTP_HDR + OLD_NAMESPACES[5]);
                                                UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[5] + "repose/"), HTTP_HDR + OLD_NAMESPACES[5]);

                                                // Add the "repose" is at the end of each.
                                                UnmarshallerValidator.replaceAll(stringBuilder, Pattern.compile(HTTP_HDR + OLD_NAMESPACES[5]), HTTP_HDR + OLD_NAMESPACES[5] + "repose/");
                                                LOG.trace("stringBuilder.toString() = \r\n" + stringBuilder.toString());

                                                try {
                                                    doc = db.parse(new ByteArrayInputStream(stringBuilder.toString().getBytes()));
                                                } finally {
                                                    db.reset();
                                                }

                                                schema.newValidator().validate(new DOMSource(doc));
                                                return unmarshaller.unmarshal(doc);
                                            } catch (TransformerException e1) {
                                                LOG.error("Failed to transform!!! Reason: {}", e1.getLocalizedMessage());
                                                LOG.trace("", e1);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                throw e;
            }
        }
        return unmarshaller.unmarshal( doc );
    }

    public static void replaceAll(StringBuilder sb, Pattern pattern, String replacement) {
        int last = 0;
        Matcher m = pattern.matcher(sb);
        while(m.find(last)) {
            last = m.end();
            sb.replace(m.start(), last, replacement);
            m = pattern.matcher(sb);
        }
    }
}
