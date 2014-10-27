package org.openrepose.filters.translation.httpx;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.openrepose.commons.utils.io.charset.CharacterSets;
import org.openrepose.repose.httpx.v1.Headers;
import org.openrepose.repose.httpx.v1.ObjectFactory;
import org.openrepose.repose.httpx.v1.QueryParameters;
import org.openrepose.repose.httpx.v1.RequestInformation;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class HttpxMarshaller {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpxMarshaller.class);
  private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
  private static final String HTTPX_SCHEMA = "/META-INF/schema/httpx/translation-httpx.xsd";
  private static final String HTTPX_PACKAGE = "org.openrepose.repose.httpx.v1";
  private static final JAXBContext jaxbContext = getContext();
  private static final Schema schema = getSchemaSource();
  private static final ObjectPool<Marshaller> marshallerPool = new SoftReferenceObjectPool<>(
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
  private Unmarshaller unmarshaller;
  private final SAXParserFactory parserFactory;
  private final ObjectFactory objectFactory;

  public HttpxMarshaller() {
    objectFactory = new ObjectFactory();
    parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(true);
    parserFactory.setXIncludeAware(false);
    parserFactory.setValidating(true);
    parserFactory.setSchema(schema);
  }
  
  private static synchronized Schema getSchemaSource() {
    if(schema == null) {
        SchemaFactory factory = SchemaFactory.newInstance(XML_SCHEMA);
        InputStream inputStream = HttpxMarshaller.class.getResourceAsStream(HTTPX_SCHEMA);
        URL inputURL = HttpxMarshaller.class.getResource(HTTPX_SCHEMA);
        Source schemaSource = new StreamSource(inputStream, inputURL.toExternalForm());
        try {
            return factory.newSchema(schemaSource);
        } catch (SAXException ex) {
            throw new HttpxException("Unable to load HTTPX schema", ex);
        }
    } else {
        return schema;
    }
  }

  private static synchronized JAXBContext getContext() {
    if (jaxbContext == null) {
        try {
            return JAXBContext.newInstance(HTTPX_PACKAGE);
        } catch (JAXBException ex) {
            throw new HttpxException("Error creating JAXBContext for HTTPX", ex);
        }
    } else {
        return jaxbContext;
    }
  }

  private synchronized Unmarshaller getUnmarshaller() {
    if (unmarshaller != null) {
      return unmarshaller;
    }

    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      unmarshaller.setSchema(schema);
      return unmarshaller;

    } catch (JAXBException ex) {
      throw new HttpxException("Unable to create HTTPX unmarshaller", ex);
    }
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
    try {

      XMLReader xmlReader = parserFactory.newSAXParser().getXMLReader();
      SAXSource source = new SAXSource(xmlReader, new InputSource(xml));
      Object result = getUnmarshaller().unmarshal(source);
      if (result instanceof JAXBElement) {
        JAXBElement element = (JAXBElement) result;
        return (T) element.getValue();
      }
      return (T) result;
    } catch (SAXException | ParserConfigurationException | JAXBException ex) {
      throw new HttpxException("Error unmarshalling xml input", ex);
    }
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

  private InputStream marshall(Object o) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    marshall(o, out);

    if (LOG.isDebugEnabled()) {
      LOG.debug(new String(out.toByteArray(),CharacterSets.UTF_8));
    }

    return new ByteArrayInputStream(out.toByteArray());
  }

  private void marshall(Object o, OutputStream out) {
      Marshaller pooledObject = null;
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
      } catch (HttpxException ex) {
          throw ex;
      } catch (Exception e) {
        LOG.error("Error marshalling HTTPX object", e);
    }
  }
}
