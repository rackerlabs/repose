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
package org.openrepose.core.services.opentracing;

import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * TracerExtractor - implements TextMap to allow for span value extraction from Repose's HttpServletRequestWrapper
 *
 * For more information, see {@link io.opentracing.Tracer#extract(Format, Object)}
 */
public class TracerExtractor implements TextMap {
    private static final Logger LOG = LoggerFactory.getLogger(TracerExtractor.class);

    /**
     * Support for multi value Map due to a header having 1+ values
     */
    private Map<String, List<String>> headers;


    public TracerExtractor(HttpServletRequestWrapper httpServletRequestWrapper) throws IOException{
        this.headers = servletHeadersToMultiMap(httpServletRequestWrapper);
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        LOG.trace("iterate through multimap");
        return new MultivaluedMapFlatIterator<>(this.headers.entrySet());
    }

    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException("This class should be used only with Tracer.inject()!");
    }


    /**
     * Convert headers into a multivalue map.  Iterates through header names and adds the headers with the
     * same key to a list
     * @param httpServletRequest {@link org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper}
     * @return {@link Map} of header names mapped to list of header values
     */
    protected Map<String, List<String>> servletHeadersToMultiMap(HttpServletRequestWrapper httpServletRequest) {
        LOG.trace("convert servlet headers to multimap");
        Map<String, List<String>> headersResult = new HashMap<>();

        Enumeration<String> headerNamesIterator = httpServletRequest.getHeaderNames();
        while (headerNamesIterator.hasMoreElements()) {
            String headerName = headerNamesIterator.nextElement();

            Enumeration<String> valuesIterator = httpServletRequest.getHeaders(headerName);
            List<String> valuesList = new ArrayList<>();
            while (valuesIterator.hasMoreElements()) {
                valuesList.add(valuesIterator.nextElement());
            }

            headersResult.put(headerName, valuesList);
        }

        return headersResult;
    }

    /**
     * Map iterator for multivalue map
     * @param <K> String
     * @param <V> String
     */
    public static final class MultivaluedMapFlatIterator<K, V> implements Iterator<Map.Entry<K, V>> {

        private final Iterator<Map.Entry<K, List<V>>> mapIterator;
        private Map.Entry<K, List<V>> mapEntry;
        private Iterator<V> listIterator;

        public MultivaluedMapFlatIterator(Set<Map.Entry<K, List<V>>> multiValuesEntrySet) {
            this.mapIterator = multiValuesEntrySet.iterator();
        }

        @Override
        public boolean hasNext() {
            if (listIterator != null && listIterator.hasNext()) {
                return true;
            }

            return mapIterator.hasNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            if (mapEntry == null || (!listIterator.hasNext() && mapIterator.hasNext())) {
                mapEntry = mapIterator.next();
                listIterator = mapEntry.getValue().iterator();
            }

            if (listIterator.hasNext()) {
                return new AbstractMap.SimpleImmutableEntry<>(mapEntry.getKey(), listIterator.next());
            } else {
                return new AbstractMap.SimpleImmutableEntry<>(mapEntry.getKey(), null);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
