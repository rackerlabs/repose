package org.openrepose.filters.translation.httpx;

import org.apache.commons.pool.ObjectPool;
import org.openrepose.commons.utils.io.charset.CharacterSets;
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

public class HttpxMarshaller {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpxMarshaller.class);
  private final SAXParserFactory parserFactory;
  private final ObjectFactory objectFactory;

  public HttpxMarshaller() {
    objectFactory = new ObjectFactory();
    parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(true);
    parserFactory.setXIncludeAware(false);
    parserFactory.setValidating(true);
    parserFactory.setSchema(HttpxMarshallerUtility.schema);
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
    ObjectPool<Unmarshaller> unmarshallerPool = HttpxMarshallerUtility.unmarshallerPool;
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
            throw new HttpxException("Error unmarshalling xml input", ex);
        } finally {
            if (pooledObject != null) {
                unmarshallerPool.returnObject(pooledObject);
            }
        }
    } catch (Exception e) {
        LOG.error("Error unmarshalling xml input", e);
        throw new HttpxException("Error unmarshalling xml input", e);
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
      LOG.debug(new String(out.toByteArray(),CharacterSets.UTF_8));
    }

    return new ByteArrayInputStream(out.toByteArray());
  }

  private void marshall(Object o, OutputStream out) {
      Marshaller pooledObject = null;
      ObjectPool<Marshaller> marshallerPool = HttpxMarshallerUtility.marshallerPool;
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
