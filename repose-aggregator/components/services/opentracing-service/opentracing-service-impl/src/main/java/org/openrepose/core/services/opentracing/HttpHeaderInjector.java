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

import java.util.*;

/**
 * HttpHeaderExtractor - implements TextMap to allow for header injection into spans
 *
 * For more information, see {@link io.opentracing.Tracer#extract(Format, Object)}
 */
public class HttpHeaderInjector implements TextMap {

    private HttpServletRequestWrapper httpServletRequestWrapper;
    private Map<String, String> headers;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpHeaderInjector.class);

    public HttpHeaderInjector(HttpServletRequestWrapper httpServletRequestWrapper) {
        this.httpServletRequestWrapper = httpServletRequestWrapper;

    }

    public HttpHeaderInjector(Map<String, String> headers) {
        this.headers = headers;
    }


    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("Should be used only with tracer#inject()");
    }

    @Override
    public void put(String key, String value) {

        if (this.httpServletRequestWrapper != null)
            this.httpServletRequestWrapper.addHeader(key, value);
        else {
            try {
                this.headers.put(key, value);
            } catch (Exception e) {
                LOG.error("Unable to add a trace header.  Key: " + key + ", value: " + value);
            }
        }
    }
}
