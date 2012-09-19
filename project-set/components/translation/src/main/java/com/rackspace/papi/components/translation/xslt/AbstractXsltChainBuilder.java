package com.rackspace.papi.components.translation.xslt;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

public abstract class AbstractXsltChainBuilder<T> implements XsltChainBuilder<T> {
    private static final String CLASSPATH_PREFIX = "classpath://";
    private final SAXTransformerFactory factory;
    public AbstractXsltChainBuilder(SAXTransformerFactory factory) {
        this.factory = factory;
        addUriResolvers();
    }
    
    public SAXTransformerFactory getFactory() {
        return factory;
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

    @Override
    public abstract XsltChain<T> build(StyleSheetInfo... stylesheets) throws XsltException;
    
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
                    Logger.getLogger(AbstractXsltChainBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        return source;
    }
}
