package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class UnmarshallerConstructionStrategy implements ConstructionStrategy<Unmarshaller> {

    private final JAXBContext context;

    public UnmarshallerConstructionStrategy(JAXBContext context) {
        this.context = context;
    }
    
    @Override
    public Unmarshaller construct() {
        try {
            return context.createUnmarshaller();
        } catch(JAXBException jaxbe) {
            throw new ResourceConstructionException("Failed to construct JAXB unmarshaller. Reason: " + jaxbe.getMessage(), jaxbe);
        }
    }
}
