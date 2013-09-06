package com.rackspace.papi.components.translation.resolvers;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.util.UriUtils;

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
        try {
            return PREFIX + UriUtils.encodePathSegment(inputStreamReference.toString(), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            return PREFIX + inputStreamReference.toString();
        }
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
            try {
                return new StreamSource(stream, new URI(href).toString());
            } catch (URISyntaxException ex) {
                return new StreamSource(stream);
            }
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
