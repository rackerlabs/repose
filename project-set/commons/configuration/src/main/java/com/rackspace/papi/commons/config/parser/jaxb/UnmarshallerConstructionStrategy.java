package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import com.rackspace.papi.commons.validate.xsd.JAXBValidator;
import java.net.URL;
import java.util.HashSet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class UnmarshallerConstructionStrategy implements ConstructionStrategy<Unmarshaller> {

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
    public Unmarshaller construct() {
        try {
            

            Unmarshaller unMarshaller=context.createUnmarshaller();
             
            if(xsdStreamSource!=null){
               
               SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
               factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
                
                Schema schema = factory.newSchema(xsdStreamSource);
                if(schema!=null){
                    unMarshaller.setSchema(schema);
                    unMarshaller.setEventHandler(new JAXBValidator());
                }
                
                           
            }
            
           return unMarshaller;
           
        } catch(JAXBException jaxbe) {
            throw new ResourceConstructionException("Failed to construct JAXB unmarshaller. Reason: " + jaxbe.getMessage(), jaxbe);
        }catch(SAXException ex){
            LOG.error("Error validating XML file", ex);
        }
        return null;
        
      
          
    }
}
