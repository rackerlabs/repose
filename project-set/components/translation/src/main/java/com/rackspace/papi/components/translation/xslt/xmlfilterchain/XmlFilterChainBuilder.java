package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.TranslationHandlerFactory;
import com.rackspace.papi.components.translation.resolvers.*;
import com.rackspace.papi.components.translation.xslt.StyleSheetInfo;
import com.rackspace.papi.components.translation.xslt.XsltException;
import net.sf.saxon.lib.FeatureKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class XmlFilterChainBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(XmlFilterChainBuilder.class);
  private static final String CLASSPATH_PREFIX = "classpath://";
  private final SAXTransformerFactory factory;

    //
    // TODO_SAXON_WORKAROUND
    // A bug in SaxonEE 9.4 causes a license to be required for IdentityTransformer, even when it is not required.
    // This is fixed in 9.5, until then we use the XalanC Transformer in place of SaxonHE for Identity transforms.
    // This same procedure is used in api-checker to get around this issue.
    // When adding SaxonEE 9.5, need to update all sections tagged with TODO_SAXON_WORKAROUND
    //
    private static final String XALANC_FACTORY_NAME = "org.apache.xalan.xsltc.trax.TransformerFactoryImpl";
    private static SAXTransformerFactory XALANC_TRANSFORMER_FACTORY;
    static {

        try {

            XALANC_TRANSFORMER_FACTORY =
                    (SAXTransformerFactory) TransformerFactory.newInstance( XALANC_FACTORY_NAME, null );

            XALANC_TRANSFORMER_FACTORY.setFeature( XMLConstants.FEATURE_SECURE_PROCESSING, true );

        } catch (TransformerConfigurationException ex) {

            LOG.error( "Error", ex );
        }
    }
    //
    //

  private final boolean allowEntities;
  private final boolean allowDtdDeclarations;

  public XmlFilterChainBuilder(SAXTransformerFactory factory, boolean allowEntities, boolean allowDeclarations) {
    this.factory = factory;
    this.allowEntities = allowEntities;
    this.allowDtdDeclarations = allowDeclarations;
    try {
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS, Boolean.TRUE);
    } catch (TransformerConfigurationException ex) {
      LOG.error("Error", ex);
    }
    addUriResolvers();
  }


  //
  // TODO_SAXON_WORKAROUND
  // A bug in SaxonEE 9.4 causes a license to be required for IdentityTransformer, even when it is not required.
  // This is fixed in 9.5, until then we use the XalanC Transformer in place of SaxonHE for Identity transforms.
  // This same procedure is used in api-checker to get around this issue.
  // When adding SaxonEE 9.5, need to update all sections tagged with TODO_SAXON_WORKAROUND
  //
  private SAXTransformerFactory getFactoryForIdentityTransform() {

      return factory.getClass().getCanonicalName().equals(TranslationHandlerFactory.SAXON_HE_FACTORY_NAME ) ?
              XALANC_TRANSFORMER_FACTORY : factory;
  }
  //

  private void addUriResolvers() {
    URIResolver resolver = factory.getURIResolver();
    if (!(resolver instanceof SourceUriResolver)) {
      SourceUriResolverChain chain = new SourceUriResolverChain(resolver);
      chain.addResolver(new InputStreamUriParameterResolver());
      chain.addResolver(new ClassPathUriResolver());
      chain.addResolver(new HttpxUriInputParameterResolver());
      factory.setURIResolver(chain);
    }
  }

  public XmlFilterChain build(StyleSheetInfo... stylesheets) throws XsltException {
    try {
      List<XmlFilterReference> filters = new ArrayList<XmlFilterReference>();
      XMLReader lastReader = getSaxReader();

      if (stylesheets.length > 0) {
        for (StyleSheetInfo resource : stylesheets) {
          Source source = getStylesheetSource(resource);
          // Wire the output of the reader to filter1 (see Note #3)
          // and the output of filter1 to filter2
          XMLFilter filter;
          try {
            filter = factory.newXMLFilter(source);
          } catch (TransformerConfigurationException ex) {
            LOG.error("Error creating XML Filter for " + resource.getUri(), ex);
            throw new XsltException(ex);
          }
          filter.setParent(lastReader);
          filters.add(new XmlFilterReference(resource.getId(), filter));
          lastReader = filter;
        }
      } else {
        filters.add(new XmlFilterReference(null, lastReader));
      }

      //
      // TODO_SAXON_WORKAROUND
      // A bug in SaxonEE 9.4 causes a license to be required for IdentityTransformer, even when it is not required.
      // This is fixed in 9.5, until then we use the XalanC Transformer in place of SaxonHE for Identity transforms.
      // This same procedure is used in api-checker to get around this issue.
      // When adding SaxonEE 9.5, need to update all sections tagged with TODO_SAXON_WORKAROUND
      //
      return new XmlFilterChain( getFactoryForIdentityTransform(), filters );
      //return new XmlFilterChain(factory, filters);

    } catch (ParserConfigurationException ex) {
      throw new XsltException(ex);
    } catch (SAXException ex) {
      throw new XsltException(ex);
    }

  }

  protected StreamSource getClassPathResource(String path) {
    String resource = path.substring(CLASSPATH_PREFIX.length());
    InputStream input = getClass().getResourceAsStream(resource);
    if (input != null) {
      return new StreamSource(input);
    }

    throw new XsltException("Unable to load stylesheet " + path);
  }

  private StreamSource nodeToStreamSource(Node node, String systemId) {
    try {
      // Create dom source for the document
      DOMSource domSource = new DOMSource(node, systemId);

      // Create a string writer
      StringWriter stringWriter = new StringWriter();

      // Create the result stream for the transform
      StreamResult result = new StreamResult(stringWriter);

      // Create a Transformer to serialize the document

        //
        // TODO_SAXON_WORKAROUND
        // A bug in SaxonEE 9.4 causes a license to be required for IdentityTransformer, even when it is not required.
        // This is fixed in 9.5, until then we use the XalanC Transformer in place of SaxonHE for Identity transforms.
        // This same procedure is used in api-checker to get around this issue.
        // When adding SaxonEE 9.5, need to update all sections tagged with TODO_SAXON_WORKAROUND
        //
        Transformer transformer = getFactoryForIdentityTransform().newTransformer();
//      Transformer transformer = factory.newTransformer();

      // Transform the document to the result stream
      transformer.transform(domSource, result);
      StringReader reader = new StringReader(stringWriter.toString());
      return new StreamSource(reader);
    } catch (TransformerException ex) {
      throw new XsltException(ex);
    }
  }

  protected Source getStylesheetSource(StyleSheetInfo stylesheet) {

    if (stylesheet.getXsl() != null) {
      return nodeToStreamSource(stylesheet.getXsl(), stylesheet.getSystemId());
    } else if (stylesheet.getUri() != null) {
      if (stylesheet.getUri().startsWith(CLASSPATH_PREFIX)) {
        return getClassPathResource(stylesheet.getUri());
      } else {
        try {
          return new StreamSource(new URL(stylesheet.getUri()).openStream());
        } catch (IOException ex) {
          throw new XsltException("Unable to load stylesheet: " + stylesheet.getUri(), ex);
        }
      }
    }

    throw new IllegalArgumentException("No stylesheet specified for " + stylesheet.getId());
  }

  protected XMLReader getSaxReader() throws ParserConfigurationException, SAXException {
    System.setProperty("entityExpansionLimit","10"); 
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setXIncludeAware(false);
    spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    spf.setValidating(true);
    spf.setNamespaceAware(true);
    spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !allowDtdDeclarations);
    SAXParser parser = spf.newSAXParser();
    XMLReader reader = parser.getXMLReader();
    reader.setEntityResolver(new ReposeEntityResolver(reader.getEntityResolver(), allowEntities));

    LOG.info("SAXParserFactory class: " + spf.getClass().getCanonicalName());
    LOG.info("SAXParser class: " + parser.getClass().getCanonicalName());
    LOG.info("XMLReader class: " + reader.getClass().getCanonicalName());

    return reader;
  }
}
