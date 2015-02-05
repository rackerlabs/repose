package org.openrepose.commons.config.parser.jaxb;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.openrepose.commons.utils.pooling.ResourceConstructionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.net.URL;

/**
 * This uses the { @link http://en.wikipedia.org/wiki/Strategy_pattern  Strategy } pattern to parameterize the creation
 * of  { @link org.openrepose.commons.config.parser.jaxb.UnmarshallerValidator UnmarshallerValidator}.
 */
public class UnmarshallerPoolableObjectFactory extends BasePoolableObjectFactory<UnmarshallerValidator> {

    private final JAXBContext context;

    private static final Logger LOG = LoggerFactory.getLogger(UnmarshallerPoolableObjectFactory.class);

    private final URL xsdStreamSource;
    private final ClassLoader classLoader;

    public UnmarshallerPoolableObjectFactory(JAXBContext context, URL xsdStreamSource, ClassLoader classLoader) {
        this.context = context;
        this.xsdStreamSource = xsdStreamSource;
        this.classLoader = classLoader;
    }

    @Override
    public UnmarshallerValidator makeObject() {
        try {
            UnmarshallerValidator uv = new UnmarshallerValidator(context);
            if (xsdStreamSource != null) {
                //TODO: this might need to have a classloader
                SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
                factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
                Schema schema = factory.newSchema(xsdStreamSource);
                uv.setSchema(schema);
            }
            return uv;
        } catch (ParserConfigurationException pce) {
            throw new ResourceConstructionException("Failed to configure DOM parser.", pce);
        } catch (JAXBException jaxbe) {
            throw new ResourceConstructionException("Failed to construct JAXB unmarshaller.", jaxbe);
        } catch (SAXException ex) {
            LOG.error("Error validating XML file", ex);
        }
        return null;
    }
}
