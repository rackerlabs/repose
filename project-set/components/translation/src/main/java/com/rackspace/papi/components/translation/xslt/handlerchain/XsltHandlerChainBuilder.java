package com.rackspace.papi.components.translation.xslt.handlerchain;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

public class XsltHandlerChainBuilder {

    private static final String CLASSPATH_PREFIX = "classpath:";
    private final SAXTransformerFactory factory;

    public XsltHandlerChainBuilder(SAXTransformerFactory factory) {
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

    public XsltHandlerChain build(String... stylesheets) throws XsltHandlerException {
        List<TransformerHandler> handlers = new ArrayList<TransformerHandler>();
        TransformerHandler lastHandler = null;

        try {
            for (StreamSource resource : getStylesheets(stylesheets)) {
                TransformerHandler handler = factory.newTransformerHandler(resource);
                if (lastHandler != null) {
                    lastHandler.setResult(new SAXResult(handler));
                }
                handlers.add(handler);
                lastHandler = handler;
            }

            if (handlers.isEmpty()) {
                lastHandler = factory.newTransformerHandler();
                handlers.add(lastHandler);
            }
        } catch (TransformerConfigurationException ex) {
            throw new XsltHandlerException(ex);
        }

        return new XsltHandlerChain(factory, handlers);
    }

    protected StreamSource getClassPathResource(String path) {
        String resource = path.substring(CLASSPATH_PREFIX.length());
        InputStream input = getClass().getResourceAsStream(resource);
        if (input != null) {
            return new StreamSource(input);
        }

        return null;
    }

    protected List<StreamSource> getStylesheets(String... stylesheets) {
        List<StreamSource> styles = new ArrayList<StreamSource>();

        for (String stylesheet : stylesheets) {
            StreamSource source;
            if (stylesheet.startsWith(CLASSPATH_PREFIX)) {
                source = getClassPathResource(stylesheet);
            } else {
                source = new StreamSource(stylesheet);
            }

            if (source != null) {
                styles.add(source);
            }
        }

        return styles;
    }
}
