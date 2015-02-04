package org.openrepose.filters.translation.httpx;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.xml.sax.SAXException;

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
    private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    private static final String HTTPX_SCHEMA = "/META-INF/schema/httpx/translation-httpx.xsd";
    private static final String HTTPX_PACKAGE = "org.openrepose.docs.repose.httpx.v1";
    private static final JAXBContext jaxbContext = getContext();
    public static final Schema schema = getSchemaSource();
    public static final ObjectPool<Marshaller> marshallerPool = new SoftReferenceObjectPool<>(
            new BasePoolableObjectFactory<Marshaller>() {
                @Override
                public Marshaller makeObject() {
                    try {
                        Marshaller marshaller = jaxbContext.createMarshaller();
                        marshaller.setSchema(schema);
                        return marshaller;
                    } catch (JAXBException ex) {
                        throw new HttpxException("Unable to create HTTPX marshaller", ex);
                    }
                }
            }
    );
    public static final ObjectPool<Unmarshaller> unmarshallerPool = new SoftReferenceObjectPool<>(
            new BasePoolableObjectFactory<Unmarshaller>() {
                @Override
                public Unmarshaller makeObject() {
                    try {
                        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                        unmarshaller.setSchema(schema);
                        return unmarshaller;

                    } catch (JAXBException ex) {
                        throw new HttpxException("Unable to create HTTPX unmarshaller", ex);
                    }
                }
            }
    );

    private static Schema getSchemaSource() {
        SchemaFactory factory = SchemaFactory.newInstance(XML_SCHEMA);
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
            return JAXBContext.newInstance(HTTPX_PACKAGE);
        } catch (JAXBException ex) {
            throw new HttpxException("Error creating JAXBContext for HTTPX", ex);
        }
    }

    private HttpxMarshallerUtility() {  }
}
