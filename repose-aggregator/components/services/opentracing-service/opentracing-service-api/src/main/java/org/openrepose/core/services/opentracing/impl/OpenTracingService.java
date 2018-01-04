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
package org.openrepose.core.services.opentracing.impl;

import java.util.Map;
import io.opentracing.Scope;
import io.opentracing.Tracer;

/**
 * OpenTracingService - service that integrates OpenTracing standards into Repose
 */
public interface OpenTracingService {

    boolean isEnabled();

    Tracer getGlobalTracer();

    /**
     * Get current ActiveSpan.  This could be either the root span (initial request), a new span of a parent
     * active span, or a sibling span (a span that's not a direct descendant or a parent span but is related
     * to it somehow.
     * <p/>
     * For more information, https://github.com/opentracing/specification/blob/master/specification.md
     * <p/>
     *
     * Span will have:
     *
     * * Operation name
     * * Start timestamp
     * * Finish timestamp
     * * (optional) Span tags
     * * (optional) Span logs
     *
     * @return a ActiveSpan which corresponds to the clientId parameter
     */
    Scope getActiveSpan();


    /**
     * Get current ActiveSpan.  This could be either the root span (initial request), a new span of a parent
     * active span, or a sibling span (a span that's not a direct descendant or a parent span but is related
     * to it somehow.
     * <p/>
     * For more information, https://github.com/opentracing/specification/blob/master/specification.md
     * <p/>
     *
     * Span will have:
     *
     * * Operation name
     * * Start timestamp
     * * Finish timestamp
     * * (optional) Span tags
     * * (optional) Span logs
     *
     * @param spanName String that maps to a name of the span
     * @param ignoreParent Boolean to ignore parent span and start a new root span
     * @param tags Optional list of tags to add to span
     * @return a ActiveSpan which corresponds to the clientId parameter
     */
    Scope startNewSpan(String spanName, boolean ignoreParent, Map<String, String> tags);

}
