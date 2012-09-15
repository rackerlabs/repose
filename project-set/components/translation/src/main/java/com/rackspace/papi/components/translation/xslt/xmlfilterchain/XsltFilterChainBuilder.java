package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import com.rackspace.papi.components.translation.xslt.StyleSheetInfo;
import com.rackspace.papi.components.translation.xslt.handlerchain.XsltHandlerChainBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

public class XsltFilterChainBuilder {

    private static final String CLASSPATH_PREFIX = "classpath:";
    private final SAXTransformerFactory factory;

    public XsltFilterChainBuilder(SAXTransformerFactory factory) {
        this.factory = factory;
        addUriResolvers();
    }

    private void addUriResolvers() {
        URIResolver resolver = factory.getURIResolver();
        if (resolver == null || !(resolver instanceof SourceUriResolver)) {
            SourceUriResolverChain chain = new SourceUriResolverChain(resolver);
            chain.addResolver(new InputStreamUriParameterResolver());
            chain.addResolver(new ClassPathUriResolver());
            factory.setURIResolver(chain);
        }
    }

    public XsltFilterChain build(StyleSheetInfo... stylesheets) throws XsltFilterException {
        try {
            List<XmlFilterReference> filters = new ArrayList<XmlFilterReference>();
            XMLReader lastReader = getSaxReader();

            for (StyleSheetInfo resource : stylesheets) {
                StreamSource source = getStylesheetSource(resource);
                // Wire the output of the reader to filter1 (see Note #3)
                // and the output of filter1 to filter2
                XMLFilter filter = factory.newXMLFilter(source);
                filter.setParent(lastReader);
                filters.add(new XmlFilterReference(resource.getId(), filter));
                lastReader = filter;
            }

            return new XsltFilterChain(factory, filters);
        } catch (TransformerConfigurationException ex) {
            throw new XsltFilterException(ex);
        } catch (ParserConfigurationException ex) {
            throw new XsltFilterException(ex);
        } catch (SAXException ex) {
            throw new XsltFilterException(ex);
        }

    }

    protected StreamSource getClassPathResource(String path) {
        String resource = path.substring(CLASSPATH_PREFIX.length());
        InputStream input = getClass().getResourceAsStream(resource);
        if (input != null) {
            return new StreamSource(input);
        }

        return null;
    }

    protected StreamSource getStylesheetSource(StyleSheetInfo stylesheet) {

            StreamSource source;
            if (stylesheet.getUri().startsWith(CLASSPATH_PREFIX)) {
                source = getClassPathResource(stylesheet.getUri());
            } else {
                try {
                    source = new StreamSource(new URL(stylesheet.getUri()).openStream());
                } catch (IOException ex) {
                    source = null;
                    Logger.getLogger(XsltHandlerChainBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        return source;
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
