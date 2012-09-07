package com.rackspace.papi.components.translation.resolvers;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

public class SourceUriResolver implements URIResolver {

    private final URIResolver parent;

    public SourceUriResolver() {
        this.parent = null;
    }

    public SourceUriResolver(URIResolver parent) {
        this.parent = parent;
    }
    
    public URIResolver getParent() {
        return parent;
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {

        if (parent != null) {
            return parent.resolve(href, base);
        }

        return null;
    }
}
