package com.rackspace.papi.components.translation.resolvers;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

public class ClassPathUriResolver extends SourceUriResolver {

    private static final String CLASSPATH_PREFIX = "classpath:";

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
            return new StreamSource(getClass().getResourceAsStream(path));
        }
        
        return super.resolve(href, base);
    }
}
