package com.rackspace.papi.components.translation.resolvers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.net.URISyntaxException;

public class ClassPathUriResolver extends SourceUriResolver {

    public static final Logger LOG = LoggerFactory.getLogger(ClassPathUriResolver.class);
    public static final String CLASSPATH_PREFIX = "classpath://";

    public ClassPathUriResolver() {
        super();
    }

    public ClassPathUriResolver(URIResolver parent) {
        super(parent);
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {

        if (href != null && href.toLowerCase().startsWith(CLASSPATH_PREFIX)) {
            String path = href.substring(CLASSPATH_PREFIX.length());
            InputStream resource = getClass().getResourceAsStream(path);
            if (resource == null) {
                return null;
            }

            try {
                return new StreamSource(resource, getClass().getResource(path).toURI().toString());
            } catch (URISyntaxException ex) {
                LOG.trace("Unable to convert resource to URI", ex);
                return new StreamSource(resource);
            }
        }
        
        return super.resolve(href, base);
    }
}
