package com.rackspace.papi.components.translation.resolvers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import net.sf.saxon.lib.OutputURIResolver;

public class OutputStreamUriParameterResolver implements OutputURIResolver {

    private final static String PREFIX = "reference:jio:";
    private final Map<String, OutputStream> streams = new HashMap<String, OutputStream>();
    private final OutputURIResolver parent;

    public OutputStreamUriParameterResolver() {
        this.parent = null;
    }

    public OutputStreamUriParameterResolver(OutputURIResolver parent) {
        this.parent = parent;
    }

    public String addStream(OutputStream outputStreamReference, String name) {
        String key = getHref(name);
        streams.put(key, outputStreamReference);
        return key;
    }

    public String getHref(String name) {
        return PREFIX + name;
    }

    @Override
    public Result resolve(String href, String base) throws TransformerException {
        OutputStream stream = streams.get(href);
        if (stream != null) {
            return new StreamResult(stream);
        }

        if (parent != null && href != null && !href.startsWith("reference")) {
            return parent.resolve(href, base);
        }

        throw new RuntimeException("Failed to resolve href: " + href);
    }

    public void close(Result result) throws TransformerException {
        try {
            ((StreamResult) result).getOutputStream().close();
        } catch (IOException ex) {
            throw new TransformerException(ex);
        }
    }
}
