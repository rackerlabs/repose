package com.rackspace.papi.components.translation.resolvers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

public class InputStreamUriParameterResolver extends SourceUriResolver {

    private static final String PREFIX = "reference:jio:";
    private final Map<String, InputStream> streams = new HashMap<String, InputStream>();
    private final List<URIResolver> resolvers = new ArrayList<URIResolver>();

    public InputStreamUriParameterResolver() {
        super();
    }

    public InputStreamUriParameterResolver(URIResolver parent) {
        super(parent);
    }

    public void addResolver(URIResolver resolver) {
        resolvers.add(resolver);
    }

    public String addStream(InputStream inputStreamReference) {
        String key = getHref(inputStreamReference);
        streams.put(key, inputStreamReference);
        return key;
    }

    public String addStream(InputStream inputStreamReference, String name) {
        String key = getHref(name);
        streams.put(key, inputStreamReference);
        return key;
    }

    public void removeStream(InputStream inputStreamReference) {
        String key = getHref(inputStreamReference);
        removeStream(key);
    }

    public void removeStream(String name) {
        streams.remove(name);
    }

    public String getHref(InputStream inputStreamReference) {
        return PREFIX + inputStreamReference.toString();
    }

    public String getHref(String name) {
        return PREFIX + name;
    }
    
    public void clearStreams() {
      streams.clear();
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        InputStream stream = streams.get(href);
        if (stream != null) {
            return new StreamSource(stream);
        }

        if (!resolvers.isEmpty()) {
            for (URIResolver resolver : resolvers) {
                Source source = resolver.resolve(href, base);
                if (source != null) {
                    return source;
                }
            }

        }
        
        return super.resolve(href, base);
    }
}
