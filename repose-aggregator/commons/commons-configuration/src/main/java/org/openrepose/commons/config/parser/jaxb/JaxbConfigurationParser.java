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

import org.apache.commons.io.IOUtils;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.openrepose.commons.config.parser.common.AbstractConfigurationObjectParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Contains a {@link org.apache.commons.pool.PoolableObjectFactory Pool} of {@link org.openrepose.commons.config.parser.jaxb.UnmarshallerValidator
 * UnmarshallerValidator} which can be used to unmmarshal JAXB XML for a given type T.
 *
 * @param <T> The configuration class which is unmarshalled
 */
public class JaxbConfigurationParser<T> extends AbstractConfigurationObjectParser<T> {

    private static final Logger LOG = LoggerFactory.getLogger(JaxbConfigurationParser.class);
    private static final EnvironmentConfiguration ENV_CONF = EnvironmentConfigurationBuilder
        .configuration()
            .parser()
                .syntax()
                    .withStartOutput("{$").withEndOutput("$}")
                .and()
            .and()
        .build();

    private final ObjectPool<UnmarshallerValidator> objectPool;

    public JaxbConfigurationParser(Class<T> configurationClass, JAXBContext jaxbContext, URL xsdStreamSource) {
        super(configurationClass);
        objectPool = new SoftReferenceObjectPool<>(new UnmarshallerPoolableObjectFactory(jaxbContext, xsdStreamSource));
    }

    /**
     * Creates a jaxb parser for a specific classloader.
     * Throws up the JAXB exception so that things know they have to handle it.
     * Moved from a "factory" class that was just a collection of static methods
     *
     * @param configurationClass
     * @param xsdStreamSource
     * @param loader
     * @param <T>
     * @return
     * @throws javax.xml.bind.JAXBException
     */
    public static <T> JaxbConfigurationParser<T> getXmlConfigurationParser(Class<T> configurationClass, URL xsdStreamSource, ClassLoader loader) throws JAXBException {
        if (xsdStreamSource == null) {
            LOG.warn("Creating a JAXB Parser Pool without any schema to validate for {}", configurationClass);
            if (LOG.isDebugEnabled()) {
                Exception tracer = new Exception("Repose Devs might care about this trace");
                LOG.debug("Logging the current stack to find where a parser pool is created without a validator", tracer);
            }
        }
        final JAXBContext context = JAXBContext.newInstance(configurationClass.getPackage().getName(), loader);
        return new JaxbConfigurationParser<>(configurationClass, context, xsdStreamSource);
    }

    @Override
    public T read(ConfigurationResource cr) {
        Object rtn = null;
        UnmarshallerValidator pooledObject;
        try {
            pooledObject = objectPool.borrowObject();
            try {
                final Object unmarshalledObject = pooledObject.validateUnmarshal(reifyConf(cr.newInputStream()));
                if (unmarshalledObject instanceof JAXBElement) {
                    rtn = ((JAXBElement) unmarshalledObject).getValue();
                } else {
                    rtn = unmarshalledObject;
                }
            } catch (IOException ioe) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.warn("This *MIGHT* be important! Unable to read configuration file: {}", ioe.getMessage());
                LOG.trace("Unable to read configuration file.", ioe);
            } catch (Exception e) {
                objectPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the UnmarshallerValidator. Reason: {}", e.getLocalizedMessage());
                LOG.trace("", e);
            } finally {
                if (pooledObject != null) {
                    objectPool.returnObject(pooledObject);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to obtain an UnmarshallerValidator. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
        }
        if (!configurationClass().isInstance(rtn)) {
            throw new ClassCastException("Parsed object from XML does not match the expected configuration class. "
                    + "Expected: " + configurationClass().getCanonicalName() + "  -  "
                    + "Actual: " + (rtn == null ? null : rtn.getClass().getCanonicalName()));
        }
        return configurationClass().cast(rtn);
    }

    // TODO: Move this into a separate parser
    private InputStream reifyConf(InputStream rawConf) throws IOException {
        JtwigModel model = JtwigModel.newModel(Collections.unmodifiableMap(System.getenv()));

        // TODO: Handle character encoding of XML configuration file
        // TODO: DocumentBuilder normally handles this, but we want to reify the template before parsing, so we cannot (easily) continue relying on it
        // TODO: XML spec for determining the character encoding https://www.w3.org/TR/REC-xml/#sec-guessing
        String template = IOUtils.toString(rawConf, StandardCharsets.UTF_8);
        String result = JtwigTemplate.inlineTemplate(template, ENV_CONF).render(model);

        return new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
    }
}
