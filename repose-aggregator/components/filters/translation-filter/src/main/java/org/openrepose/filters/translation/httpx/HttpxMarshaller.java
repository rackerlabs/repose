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

import org.apache.commons.pool.ObjectPool;
import org.openrepose.docs.repose.httpx.v1.Headers;
import org.openrepose.docs.repose.httpx.v1.ObjectFactory;
import org.openrepose.docs.repose.httpx.v1.QueryParameters;
import org.openrepose.docs.repose.httpx.v1.RequestInformation;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpxMarshaller {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpxMarshaller.class);
    private static final String ERROR_UNMARSHALLING = "Error unmarshalling xml input";
    private final SAXParserFactory parserFactory;
    private final ObjectFactory objectFactory;

    public HttpxMarshaller() {
        objectFactory = new ObjectFactory();
        parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        parserFactory.setXIncludeAware(false);
        parserFactory.setValidating(true);
        parserFactory.setSchema(HttpxMarshallerUtility.SCHEMA);
    }

    public RequestInformation unmarshallRequestInformation(InputStream xml) {
        return unmarshall(xml);
    }

    public Headers unmarshallHeaders(InputStream xml) {
        return unmarshall(xml);
    }

    public QueryParameters unmarshallQueryParameters(InputStream xml) {
        return unmarshall(xml);
    }

    public <T> T unmarshall(InputStream xml) {
        T rtnObject = null;
        Unmarshaller pooledObject = null;
        ObjectPool<Unmarshaller> unmarshallerPool = HttpxMarshallerUtility.UNMARSHALLER_POOL;
        try {
            try {
                pooledObject = unmarshallerPool.borrowObject();
                XMLReader xmlReader = parserFactory.newSAXParser().getXMLReader();
                SAXSource source = new SAXSource(xmlReader, new InputSource(xml));
                Object result = pooledObject.unmarshal(source);
                if (result instanceof JAXBElement) {
                    JAXBElement element = (JAXBElement) result;
                    rtnObject = (T) element.getValue();
                } else {
                    rtnObject = (T) result;
                }
            } catch (Exception ex) {
                unmarshallerPool.invalidateObject(pooledObject);
                pooledObject = null;
                throw new HttpxException(ERROR_UNMARSHALLING, ex);
            } finally {
                if (pooledObject != null) {
                    unmarshallerPool.returnObject(pooledObject);
                }
            }
        } catch (Exception e) {
            LOG.error(ERROR_UNMARSHALLING, e);
            throw new HttpxException(ERROR_UNMARSHALLING, e);
        }
        return rtnObject;
    }

    public InputStream marshall(RequestInformation request) {
        return marshall(objectFactory.createRequestInformation(request));
    }

    public InputStream marshall(Headers header) {
        return marshall(objectFactory.createHeaders(header));
    }

    public InputStream marshall(QueryParameters params) {
        return marshall(objectFactory.createParameters(params));
    }

    public InputStream marshall(Object o) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        marshall(o, out);

        if (LOG.isDebugEnabled()) {
            LOG.debug(new String(out.toByteArray(), StandardCharsets.UTF_8));
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void marshall(Object o, OutputStream out) {
        Marshaller pooledObject = null;
        ObjectPool<Marshaller> marshallerPool = HttpxMarshallerUtility.MARSHALLER_POOL;
        try {
            try {
                pooledObject = marshallerPool.borrowObject();
                pooledObject.marshal(o, out);
            } catch (Exception ex) {
                marshallerPool.invalidateObject(pooledObject);
                pooledObject = null;
                throw new HttpxException("Error marshalling HTTPX object", ex);
            } finally {
                if (pooledObject != null) {
                    marshallerPool.returnObject(pooledObject);
                }
            }
        } catch (Exception e) {
            throw new HttpxException("Error marshalling HTTPX object", e);
        }
    }
}
