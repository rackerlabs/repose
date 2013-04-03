package com.rackspace.papi.components.translation.httpx;

import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import org.openrepose.repose.httpx.v1.Headers;
import org.openrepose.repose.httpx.v1.ObjectFactory;
import org.openrepose.repose.httpx.v1.QueryParameters;
import org.openrepose.repose.httpx.v1.RequestInformation;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;

public class HttpxMarshaller {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpxMarshaller.class);
  private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
  private static final String HTTPX_SCHEMA = "/META-INF/schema/httpx/translation-httpx.xsd";
  private static final String HTTPX_PACKAGE = "org.openrepose.repose.httpx.v1";
  private JAXBContext jaxbContext;
  private Marshaller marshaller;
  private Unmarshaller unmarshaller;
  private final SAXParserFactory parserFactory;
  private final ObjectFactory objectFactory;
  private final DocumentBuilderFactory builderFactory;

  public HttpxMarshaller() {
    builderFactory = DocumentBuilderFactory.newInstance();
    objectFactory = new ObjectFactory();
    parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(true);
    parserFactory.setXIncludeAware(false);
    parserFactory.setValidating(true);
    parserFactory.setSchema(getSchemaSource());
  }

  private Document buildDocument(InputStream xml) {
    try {
      return builderFactory.newDocumentBuilder().parse(xml);
    } catch (SAXException ex) {
      throw new HttpxException(ex);
    } catch (IOException ex) {
      throw new HttpxException(ex);
    } catch (ParserConfigurationException ex) {
      throw new HttpxException(ex);
    }
  }
  
  private Schema getSchemaSource() {
    SchemaFactory factory = SchemaFactory.newInstance(XML_SCHEMA);
    Source schemaSource = new StreamSource(getClass().getResourceAsStream(HTTPX_SCHEMA));
    try {
      return factory.newSchema(schemaSource);
    } catch (SAXException ex) {
      throw new HttpxException("Unable to load HTTPX schema", ex);
    }
  }

  private synchronized JAXBContext getContext() {
    if (jaxbContext == null) {
      try {
        jaxbContext = JAXBContext.newInstance(HTTPX_PACKAGE);
      } catch (JAXBException ex) {
        throw new HttpxException("Error creating JAXBContext for HTTPX", ex);
      }
    }

    return jaxbContext;
  }

  private synchronized Marshaller getMarshaller() {
    if (marshaller != null) {
      return marshaller;
    }

    try {
      marshaller = getContext().createMarshaller();
      marshaller.setSchema(getSchemaSource());
      return marshaller;
    } catch (JAXBException ex) {
      throw new HttpxException("Unable to create HTTPX marshaller", ex);
    }
  }

  private synchronized Unmarshaller getUnmarshaller() {
    if (unmarshaller != null) {
      return unmarshaller;
    }

    try {
      unmarshaller = getContext().createUnmarshaller();
      unmarshaller.setSchema(getSchemaSource());
      return unmarshaller;

    } catch (JAXBException ex) {
      throw new HttpxException("Unable to create HTTPX unmarshaller", ex);
    }
  }
  
  public RequestInformation unmarshallRequestInformation(String xml) {
    return unmarshall(xml);
  }

  public RequestInformation unmarshallRequestInformation(InputStream xml) {
    return unmarshall(xml);
  }

  public Headers unmarshallHeaders(String xml) {
    return unmarshall(xml);
  }

  public Headers unmarshallHeaders(InputStream xml) {
    return unmarshall(xml);
  }

  public QueryParameters unmarshallQueryParameters(String xml) {
    return unmarshall(xml);
  }

  public QueryParameters unmarshallQueryParameters(InputStream xml) {
    return unmarshall(xml);
  }

  public <T> T unmarshall(String xml) {
    return unmarshall(new ByteArrayInputStream(xml.getBytes(CharacterSets.UTF_8)));
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
    } catch (SAXException ex) {
      throw new HttpxException("Error unmarshalling xml input", ex);
    } catch (ParserConfigurationException ex) {
      throw new HttpxException("Error unmarshalling xml input", ex);
    } catch (JAXBException ex) {
      throw new HttpxException("Error unmarshalling xml input", ex);
    }
  }

  public Document getDocument(Headers headers) {
    return buildDocument(marshall(headers));
  }
  
  public InputStream marshall(RequestInformation request) {
    return marshall(objectFactory.createRequestInformation(request));
  }
  
  public void marshall(RequestInformation request, OutputStream out) {
    marshall(objectFactory.createRequestInformation(request), out);
  }
  
  public InputStream marshall(Headers header) {
    return marshall(objectFactory.createHeaders(header));
  }

  public void marshall(Headers headers, OutputStream out) {
    marshall(objectFactory.createHeaders(headers), out);
  }

  public Document getDocument(QueryParameters params) {
    return buildDocument(marshall(params));
  }
  
  public InputStream marshall(QueryParameters params) {
    return marshall(objectFactory.createParameters(params));
  }

  public void marshall(QueryParameters params, OutputStream out) {
    marshall(objectFactory.createParameters(params), out);
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
    try {
      getMarshaller().marshal(o, out);
    } catch (JAXBException ex) {
      throw new HttpxException("Error marshalling HTTPX object", ex);
    }
  }
}
