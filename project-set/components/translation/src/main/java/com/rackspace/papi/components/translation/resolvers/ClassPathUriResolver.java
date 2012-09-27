package com.rackspace.papi.components.translation.resolvers;

import java.io.InputStream;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

public class ClassPathUriResolver extends SourceUriResolver {

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
            
            return new StreamSource(resource);
        }
        
        return super.resolve(href, base);
    }
}
