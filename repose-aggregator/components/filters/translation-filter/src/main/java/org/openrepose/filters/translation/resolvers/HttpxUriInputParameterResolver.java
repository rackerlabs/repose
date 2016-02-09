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

import org.openrepose.docs.repose.httpx.v1.Headers;
import org.openrepose.docs.repose.httpx.v1.QueryParameters;
import org.openrepose.docs.repose.httpx.v1.RequestInformation;
import org.openrepose.filters.translation.httpx.HttpxMarshaller;
import org.openrepose.filters.translation.httpx.HttpxProducer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

public class HttpxUriInputParameterResolver extends SourceUriResolver {

    public static final String HEADERS_PREFIX = "repose:input:headers";
    public static final String PARAMS_PREFIX = "repose:input:query";
    public static final String REQUEST_INFO_PREFIX = "repose:input:request";
    private final HttpxMarshaller marshaller;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpxProducer producer;
    private Headers headers;
    private QueryParameters params;
    private RequestInformation info;

    public HttpxUriInputParameterResolver() {
        super();

        marshaller = new HttpxMarshaller();
    }
    public HttpxUriInputParameterResolver(URIResolver parent) {
        super(parent);
        marshaller = new HttpxMarshaller();
    }

    public void setParams(QueryParameters params) {
        this.params = params;
    }

    public void reset() {
        request = null;
        response = null;
        producer = null;
        headers = null;
        params = null;
        info = null;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    private HttpxProducer getProducer() {
        if (producer == null) {
            producer = new HttpxProducer(request, response);
        }

        return producer;
    }

    private Headers getHeaders() {
        return headers != null ? headers : getProducer().getHeaders();
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    private RequestInformation getRequestInformation() {
        return info != null ? info : getProducer().getRequestInformation();
    }

    public void setRequestInformation(RequestInformation info) {
        this.info = info;
    }

    private QueryParameters getRequestParameters() {
        return params != null ? params : getProducer().getRequestParameters();
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {

        if (href != null) {
            if (href.startsWith(HEADERS_PREFIX)) {
                return new StreamSource(marshaller.marshall(getHeaders()));
            } else if (href.startsWith(REQUEST_INFO_PREFIX)) {
                return new StreamSource(marshaller.marshall(getRequestInformation()));
            } else if (href.startsWith(PARAMS_PREFIX)) {
                return new StreamSource(marshaller.marshall(getRequestParameters()));
            }
        }
        return super.resolve(href, base);
    }
}
