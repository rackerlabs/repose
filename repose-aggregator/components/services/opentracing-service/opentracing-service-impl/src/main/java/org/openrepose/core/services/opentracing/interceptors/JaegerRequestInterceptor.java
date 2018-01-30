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
package org.openrepose.core.services.opentracing.interceptors;

import com.uber.jaeger.httpclient.TracingRequestInterceptor;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaegerRequestInterceptor extends TracingRequestInterceptor implements RequestInterceptor {
    public JaegerRequestInterceptor(Tracer tracer) {
        super(tracer);
    }

    @Override
    protected void onSpanStarted(Span clientSpan, HttpRequest httpRequest, HttpContext httpContext) {
        Header traceRequestHeader =  httpRequest.getFirstHeader(CommonHttpHeader.REQUEST_ID);

        if (traceRequestHeader != null ) {
            clientSpan.setTag(CommonHttpHeader.REQUEST_ID, traceRequestHeader.getValue());
        }

        super.onSpanStarted(clientSpan, httpRequest, httpContext);
    }

    @Override
    protected String getOperationName(HttpRequest httpRequest) {
        return String.format("%s %s",
            httpRequest.getRequestLine().getMethod(), httpRequest.getRequestLine().getUri());
    }
}
