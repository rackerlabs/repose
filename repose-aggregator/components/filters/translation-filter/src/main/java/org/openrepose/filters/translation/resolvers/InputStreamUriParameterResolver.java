/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.translation.resolvers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

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

public class InputStreamUriParameterResolver extends SourceUriResolver {

    private static final Logger LOG = LoggerFactory.getLogger(InputStreamUriParameterResolver.class);

    private static final String PREFIX = "reference:jio:";
    private final Map<String, InputStream> streams = new HashMap<>();
    private final List<URIResolver> resolvers = new ArrayList<>();

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
        } catch (Exception ex) {
            LOG.trace("Unable to encode the path segment to utf-8", ex);
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
                LOG.trace("Unable to parse href to URI", ex);
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
