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

import io.opentracing.Tracer;

/**
 * OpenTracingService - service that integrates OpenTracing standards into Repose
 */
public interface OpenTracingService {

    /**
     * Check that the service is enabled.  The tracing will only happen if this is enabled.
     * While configuration might set the service as enabled, there are a couple instances when this might
     * get turned back to disabled:
     *
     * * Invalid tracer specified
     * * Unable to connect to tracer
     *
     * @return a Boolean which corresponds to the clientId parameter
     */
    boolean isEnabled();

    /**
     * Retrieves the global tracer singleton.  This is configured at startup via opentracing.cfg.xml tracer
     * specific configuration.  If an invalid tracer is provided, the tracer will not be available.
     * @return io.opentracing.Tracer object that contains Tracer implementation information
     */
    Tracer getGlobalTracer();

    /**
     * Retrieves service name.  This is specific for every repose implementation and defines the namespace
     * for your service.  It should be unique in your company/flow.
     * @return String object that contains your service name
     */
    String getServiceName();

    /**
     * Retrieves tracer header.  This is tracer implementation specific
     * @return Returns header name for your tracer implementation
     */
    String getTracerHeaderName();
}
