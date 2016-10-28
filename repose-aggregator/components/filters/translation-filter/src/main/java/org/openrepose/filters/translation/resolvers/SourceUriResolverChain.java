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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.util.ArrayList;
import java.util.List;

public class SourceUriResolverChain extends SourceUriResolver {

    private final List<URIResolver> resolvers = new ArrayList<>();

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
                return (T) resolver;
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
