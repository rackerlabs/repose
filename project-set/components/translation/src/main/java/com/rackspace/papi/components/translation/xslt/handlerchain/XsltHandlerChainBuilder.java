package com.rackspace.papi.components.translation.xslt.handlerchain;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public XsltHandlerChain build(StyleSheetInfo... stylesheets) throws XsltHandlerException {
        List<XslTransformer> handlers = new ArrayList<XslTransformer>();
        XslTransformer lastHandler = null;

        try {
            for (StyleSheetInfo resource : stylesheets) {
                StreamSource source = getStylesheetSource(resource);
                XslTransformer handler = new XslTransformer(resource.getId(), factory.newTransformerHandler(source));
                if (lastHandler != null) {
                    lastHandler.getHandler().setResult(new SAXResult(handler.getHandler()));
                }
                handlers.add(handler);
                lastHandler = handler;
            }

            if (handlers.isEmpty()) {
                lastHandler = new XslTransformer("", factory.newTransformerHandler());
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
}
