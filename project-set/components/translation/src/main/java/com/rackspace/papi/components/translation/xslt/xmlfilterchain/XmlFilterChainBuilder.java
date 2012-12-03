package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import com.rackspace.papi.components.translation.xslt.StyleSheetInfo;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

public class XmlFilterChainBuilder {

    private static final String CLASSPATH_PREFIX = "classpath://";
    private final SAXTransformerFactory factory;

    public XmlFilterChainBuilder(SAXTransformerFactory factory) {
        this.factory = factory;
        addUriResolvers();
    }

    private void addUriResolvers() {
        URIResolver resolver = factory.getURIResolver();
        if (!(resolver instanceof SourceUriResolver)) {
            SourceUriResolverChain chain = new SourceUriResolverChain(resolver);
            chain.addResolver(new InputStreamUriParameterResolver());
            chain.addResolver(new ClassPathUriResolver());
            factory.setURIResolver(chain);
        }
    }

    public XmlFilterChain build(StyleSheetInfo... stylesheets) throws XsltException {
        try {
            List<XmlFilterReference> filters = new ArrayList<XmlFilterReference>();
            XMLReader lastReader = getSaxReader();

            for (StyleSheetInfo resource : stylesheets) {
                Source source = getStylesheetSource(resource);
                // Wire the output of the reader to filter1 (see Note #3)
                // and the output of filter1 to filter2
                XMLFilter filter = factory.newXMLFilter(source);
                filter.setParent(lastReader);
                filters.add(new XmlFilterReference(resource.getId(), filter));
                lastReader = filter;
            }

            return new XmlFilterChain(factory, filters);
        } catch (TransformerConfigurationException ex) {
            throw new XsltException(ex);
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
            Transformer transformer = factory.newTransformer();

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
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(true);
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        XMLReader reader = parser.getXMLReader();

        return reader;
    }
}
