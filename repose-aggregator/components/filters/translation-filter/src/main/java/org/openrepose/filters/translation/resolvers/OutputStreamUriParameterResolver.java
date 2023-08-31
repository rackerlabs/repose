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

import net.sf.saxon.lib.OutputURIResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class OutputStreamUriParameterResolver implements OutputURIResolver {

    public static final Logger LOG = LoggerFactory.getLogger(OutputStreamUriParameterResolver.class);
    public static final String PREFIX = "repose:output:";
    private final Map<String, OutputStream> streams = new HashMap<>();
    private final OutputURIResolver parent;

    public OutputStreamUriParameterResolver() {
        this.parent = null;
    }

    public OutputStreamUriParameterResolver(OutputURIResolver parent) {
        this.parent = parent;
    }

    public void clearStreams() {
        streams.clear();
    }

    public String addStream(OutputStream outputStreamReference, String name) {
        String key = getHref(name);
        streams.put(key, outputStreamReference);
        return key;
    }

    public String getHref(String name) {
        try {
            return PREFIX + UriUtils.encodePathSegment(name, "utf-8");
        } catch (Exception ex) {
            LOG.trace("unable to encode path segment to utf-8", ex);
            return PREFIX + name;
        }
    }

    @Override
    public OutputURIResolver newInstance() {
        return new OutputStreamUriParameterResolver(this);
    }

    @Override
    public Result resolve(String href, String base) throws TransformerException {
        OutputStream stream = streams.get(href);
        if (stream != null) {
            StreamResult result = new StreamResult(stream);
            try {
                result.setSystemId(new URI(href).toString());
            } catch (URISyntaxException ex) {
                LOG.trace("Unable to parse URI", ex);
            }

            return result;
        }

        if (parent != null && href != null && !href.startsWith("reference")) {
            return parent.resolve(href, base);
        }

        throw new ResourceNotFoundException("Failed to resolve href: " + href);
    }

    @Override
    public void close(Result result) throws TransformerException {
        try {
            ((StreamResult) result).getOutputStream().close();
        } catch (IOException ex) {
            throw new TransformerException(ex);
        }
    }

    private static class ResourceNotFoundException extends RuntimeException {

        ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
