package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;

import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * This uses the { @link http://en.wikipedia.org/wiki/Strategy_pattern  Strategy } pattern to parameterize the creation
 * of  { @link com.rackspace.papi.commons.config.parser.jaxb.UnmarshallerValidator UnmarshallerValidator}.
 */
public class UnmarshallerConstructionStrategy implements ConstructionStrategy<UnmarshallerValidator> {

    private final JAXBContext context;
    
    private static final Logger LOG = LoggerFactory.getLogger(UnmarshallerConstructionStrategy.class);
    
    private final URL xsdStreamSource;

    public UnmarshallerConstructionStrategy(JAXBContext context) {
        this.context = context;
        xsdStreamSource=null;
     }
    
      public UnmarshallerConstructionStrategy(JAXBContext context, URL xsdStreamSource) {
        this.context = context;
        this.xsdStreamSource=xsdStreamSource;
     }
    
    
    @Override
    public UnmarshallerValidator construct() {

        try {

            UnmarshallerValidator uv = new UnmarshallerValidator( context );

            if(xsdStreamSource!=null){

                SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
                factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);

                Schema schema = factory.newSchema(xsdStreamSource);

                uv.setSchema( schema );

            }

           return uv;
           
        } catch( ParserConfigurationException pce ) {

            throw new ResourceConstructionException("Failed to configure DOM parser. Reason: " + pce.getMessage(), pce );
        } catch(JAXBException jaxbe) {
            throw new ResourceConstructionException("Failed to construct JAXB unmarshaller. Reason: " + jaxbe.getMessage(), jaxbe);
        } catch(SAXException ex){
            LOG.error("Error validating XML file", ex);
        }
        return null;
    }
}
