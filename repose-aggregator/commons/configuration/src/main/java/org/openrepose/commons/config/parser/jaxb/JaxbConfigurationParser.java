package org.openrepose.commons.config.parser.jaxb;

import org.openrepose.commons.config.parser.common.AbstractConfigurationObjectParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.net.URL;

/**
 * Contains a {@link org.apache.commons.pool.PoolableObjectFactory Pool} of {@link org.openrepose.commons.config.parser.jaxb.UnmarshallerValidator
 * UnmarshallerValidator} which can be used to unmmarshal JAXB XML for a given type T.
 *
 * @param <T> The configuration class which is unmarshalled
 */
public class JaxbConfigurationParser<T> extends AbstractConfigurationObjectParser<T> {

    private static final Logger LOG = LoggerFactory.getLogger(JaxbConfigurationParser.class);
    private final ObjectPool<UnmarshallerValidator> objectPool;

    public JaxbConfigurationParser(Class<T> configurationClass, JAXBContext jaxbContext, URL xsdStreamSource) {
        super(configurationClass);
        objectPool = new SoftReferenceObjectPool<>(new UnmarshallerPoolableObjectFactory(jaxbContext, xsdStreamSource));
    }

    @Override
    public T read(ConfigurationResource cr) {
        Object rtn = null;
        UnmarshallerValidator pooledObject = null;
        try {
            pooledObject = objectPool.borrowObject();
            try {
                final Object unmarshalledObject = pooledObject.validateUnmarshal(cr.newInputStream());
                if (unmarshalledObject instanceof JAXBElement) {
                    rtn = ((JAXBElement) unmarshalledObject).getValue();
                } else {
                    rtn = unmarshalledObject;
                }
            } catch (Exception e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the UnmarshallerValidator.", e);
            } finally {
                if (null != pooledObject) {
                    objectPool.returnObject(pooledObject);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to obtain an UnmarshallerValidator.", e);
        }
        if (!configurationClass().isInstance(rtn)) {
            throw new ClassCastException("Parsed object from XML does not match the expected configuration class. "
                    + "Expected: " + configurationClass().getCanonicalName() + "  -  "
                    + "Actual: " + rtn.getClass().getCanonicalName());
        }
        return configurationClass().cast(rtn);
    }
}
