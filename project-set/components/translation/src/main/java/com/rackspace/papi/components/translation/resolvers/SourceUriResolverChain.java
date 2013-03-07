package com.rackspace.papi.components.translation.resolvers;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.util.ArrayList;
import java.util.List;

public class SourceUriResolverChain extends SourceUriResolver {

    private final List<URIResolver> resolvers = new ArrayList<URIResolver>();

    public SourceUriResolverChain() {
        super();
    }

    public SourceUriResolverChain(URIResolver parent) {
        super(parent);
    }

    public void addResolver(URIResolver resolver) {
        resolvers.add(resolver);
    }
    
    public <T extends URIResolver> T getResolverOfType(Class<T> type) {
        for (URIResolver resolver : resolvers) {
            if (type.isAssignableFrom(resolver.getClass())) {
                return (T)resolver;
            }
        }
        
        return null;
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {

        for (URIResolver resolver : resolvers) {
            Source source = resolver.resolve(href, base);
            if (source != null) {
                return source;
            }
        }

        return super.resolve(href, base);
    }
}
