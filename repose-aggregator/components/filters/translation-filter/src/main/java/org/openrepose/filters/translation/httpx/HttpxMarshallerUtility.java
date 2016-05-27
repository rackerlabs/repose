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
package org.openrepose.filters.translation.httpx;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.net.URL;

/**
 * Helper things for HttpxMarshaller that only need to be created once
 */
public class HttpxMarshallerUtility {
    private static final String XERCES_FACTORY_NAME = "org.apache.xerces.jaxp.validation.XMLSchemaFactory";
    private static final String HTTPX_SCHEMA = "/META-INF/schema/httpx/translation-httpx.xsd";
    public static final Schema SCHEMA = getSchemaSource();
    private static final String HTTPX_PACKAGE = "org.openrepose.docs.repose.httpx.v1";
    private static final JAXBContext JAXB_CONTEXT = getContext();
    public static final ObjectPool<Marshaller> MARSHALLER_POOL = new SoftReferenceObjectPool<>(
            new BasePoolableObjectFactory<Marshaller>() {
                @Override
                public Marshaller makeObject() {
                    try {
                        Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
                        marshaller.setSchema(SCHEMA);
                        return marshaller;
                    } catch (JAXBException ex) {
                        throw new HttpxException("Unable to create HTTPX marshaller", ex);
                    }
                }
            }
    );
    public static final ObjectPool<Unmarshaller> UNMARSHALLER_POOL = new SoftReferenceObjectPool<>(
            new BasePoolableObjectFactory<Unmarshaller>() {
                @Override
                public Unmarshaller makeObject() {
                    try {
                        Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
                        unmarshaller.setSchema(SCHEMA);
                        return unmarshaller;

                    } catch (JAXBException ex) {
                        throw new HttpxException("Unable to create HTTPX unmarshaller", ex);
                    }
                }
            }
    );

    private HttpxMarshallerUtility() {
    }

    private static Schema getSchemaSource() {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI, XERCES_FACTORY_NAME, HttpxMarshallerUtility.class.getClassLoader());
        InputStream inputStream = HttpxMarshaller.class.getResourceAsStream(HTTPX_SCHEMA);
        URL inputURL = HttpxMarshaller.class.getResource(HTTPX_SCHEMA);
        Source schemaSource = new StreamSource(inputStream, inputURL.toExternalForm());
        try {
            return factory.newSchema(schemaSource);
        } catch (SAXException ex) {
            throw new HttpxException("Unable to load HTTPX schema", ex);
        }
    }

    private static JAXBContext getContext() {
        try {
            return JAXBContext.newInstance(HTTPX_PACKAGE, HttpxMarshallerUtility.class.getClassLoader());
        } catch (JAXBException ex) {
            throw new HttpxException("Error creating JAXBContext for HTTPX", ex);
        }
    }
}
