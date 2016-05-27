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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.net.URISyntaxException;

public class ClassPathUriResolver extends SourceUriResolver {

    public static final Logger LOG = LoggerFactory.getLogger(ClassPathUriResolver.class);
    public static final String CLASSPATH_PREFIX = "classpath://";

    public ClassPathUriResolver() {
        super();
    }

    public ClassPathUriResolver(URIResolver parent) {
        super(parent);
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {

        if (href != null && href.startsWith(CLASSPATH_PREFIX)) {
            String path = href.substring(CLASSPATH_PREFIX.length());
            InputStream resource = getClass().getResourceAsStream(path);
            if (resource == null) {
                return null;
            }

            try {
                return new StreamSource(resource, getClass().getResource(path).toURI().toString());
            } catch (URISyntaxException ex) {
                LOG.trace("Unable to convert resource to URI", ex);
                return new StreamSource(resource);
            }
        }

        return super.resolve(href, base);
    }
}
