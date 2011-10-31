package com.rackspace.papi.components.translation.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

public class InputStreamUriParameterResolver implements URIResolver {

    private final static String PREFIX = "reference:jio:";
    private final Map<String, InputStream> streams = new HashMap<String, InputStream>();
    private final URIResolver parent;
    
    public InputStreamUriParameterResolver(URIResolver parent) {
        this.parent = parent;
    }
    
    public String addStream(InputStream inputStreamReference) {
       String key = getHref(inputStreamReference);
       streams.put(key, inputStreamReference);
       return key;
    }
    
    public String getHref(InputStream inputStreamReference) {
        return PREFIX + inputStreamReference.toString();
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        InputStream stream = streams.get(href);
        if (stream != null) {
           return new StreamSource(stream);
        }
        
        if (parent != null && href != null && !href.startsWith("reference")) {
           return parent.resolve(href, base);
        }
        
        throw new RuntimeException("Failed to resolve href: " + href);
    }
}
