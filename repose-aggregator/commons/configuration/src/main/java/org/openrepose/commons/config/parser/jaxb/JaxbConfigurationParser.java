package org.openrepose.commons.config.parser.jaxb;

import org.openrepose.commons.config.parser.common.AbstractConfigurationObjectParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.pooling.GenericBlockingResourcePool;
import org.openrepose.commons.utils.pooling.Pool;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.net.URL;

/**
 * Contains a {@link org.openrepose.commons.utils.pooling.Pool Pool} of {@link org.openrepose.commons.config.parser.jaxb.UnmarshallerValidator
 * UnmarshallerValidator} which can be used to unmmarshal JAXB XML for a given type T.
 *
 * @param <T> The configuration class which is unmarshalled
 */
public class JaxbConfigurationParser <T> extends AbstractConfigurationObjectParser<T> {

    private final Pool<UnmarshallerValidator> marshallerPool;

    public JaxbConfigurationParser(Class<T> configurationClass, JAXBContext jaxbContext, URL xsdStreamSource) {
        super(configurationClass);
        marshallerPool = new GenericBlockingResourcePool<UnmarshallerValidator>(
                new UnmarshallerConstructionStrategy(jaxbContext,xsdStreamSource));
    }
    
    @Override
    public T read(ConfigurationResource cr) {
        final Object parsedObject = marshallerPool.use(new UnmarshallerResourceContext(cr));
        final Object returnable = parsedObject instanceof JAXBElement
                ? ((JAXBElement)parsedObject).getValue()
                : parsedObject;
                
        if (!configurationClass().isInstance(returnable)) {
             throw new ClassCastException("Parsed object from XML does not match the expected configuration class. "
                     + "Expected: " + configurationClass().getCanonicalName() + "  -  "
                     + "Actual: " + returnable.getClass().getCanonicalName());
        }
        
        return configurationClass().cast(returnable);
    }
}
