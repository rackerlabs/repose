package com.rackspace.papi.components.ratelimit.util.combine;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

public class InputStreamUriParameter implements URIResolver {

    private final InputStream inputStreamReference;
    private final String hrefSpec;
    
    public InputStreamUriParameter(InputStream inputStreamReference) {
        this.inputStreamReference = inputStreamReference;
        this.hrefSpec = "reference:jio:" + inputStreamReference.toString();
    }
    
    public String getHref() {
        return hrefSpec;
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        if (hrefSpec.equals(href)) {
            return new StreamSource(inputStreamReference);
        }
        
        throw new CombinedLimitsTransformerException("Failed to resolve href: " + href);
    }
}
