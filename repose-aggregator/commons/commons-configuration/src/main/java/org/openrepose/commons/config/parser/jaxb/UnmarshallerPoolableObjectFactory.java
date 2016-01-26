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
 * <p/>
 * TODO: do we really need a pool of objects for JAXB unmarshalling for only one XSD? This seems excessive...
 * TODO: Especially since the JAXBContext is passed in anyway! There's probably no reason for this
 */
public class UnmarshallerPoolableObjectFactory extends BasePoolableObjectFactory<UnmarshallerValidator> {

    private static final Logger LOG = LoggerFactory.getLogger(UnmarshallerPoolableObjectFactory.class);
    private final JAXBContext context;
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
            //TODO: refactor this to either use two different classes that extend an Unmarshaller...
            if (xsdStreamSource != null) {
                //TODO: this might need to have a classloader
                SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
                factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
                Schema schema = factory.newSchema(xsdStreamSource);
                //Setting the schema after the object creation is kind of gross
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
