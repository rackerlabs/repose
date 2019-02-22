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
package org.openrepose.filters.translation.httpx;

import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.servlet.http.HeaderContainer;
import org.openrepose.commons.utils.servlet.http.RequestHeaderContainer;
import org.openrepose.commons.utils.servlet.http.ResponseHeaderContainer;
import org.openrepose.docs.repose.httpx.v1.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class HttpxProducer {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private Headers headers;
    private QueryParameters queryParameters;
    private RequestInformation requestInformation;

    public HttpxProducer(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    private HeaderList getHeaderList(HeaderContainer container) {
        HeaderList result = OBJECT_FACTORY.createHeaderList();
        List<QualityNameValuePair> headerList = result.getHeader();

        for (HeaderName name : container.getHeaderNames()) {
            List<HeaderValue> values = container.getHeaderValues(name.getName());
            for (HeaderValue value : values) {
                QualityNameValuePair header = new QualityNameValuePair();

                header.setName(name.getName());
                header.setValue(value.getValue());
                header.setQuality(value.getQualityFactor());

                headerList.add(header);
            }
        }

        return result;
    }

    public RequestInformation getRequestInformation() {
        if (requestInformation == null) {
            requestInformation = OBJECT_FACTORY.createRequestInformation();
            requestInformation.setUri(request.getRequestURI());
            requestInformation.setUrl(request.getRequestURL().toString());
            ReadOnlyRequestInformation info = OBJECT_FACTORY.createReadOnlyRequestInformation();

            info.setAuthType(StringUtils.defaultIfEmpty(request.getAuthType(), ""));
            info.setContextPath(StringUtils.defaultIfEmpty(request.getContextPath(), ""));
            info.setLocalAddr(StringUtils.defaultIfEmpty(request.getLocalAddr(), ""));
            info.setLocalName(StringUtils.defaultIfEmpty(request.getLocalName(), ""));
            info.setLocalPort(request.getLocalPort());
            info.setPathInfo(StringUtils.defaultIfEmpty(request.getPathInfo(), ""));
            info.setPathTranslated(StringUtils.defaultIfEmpty(request.getPathTranslated(), ""));
            info.setProtocol(StringUtils.defaultIfEmpty(request.getProtocol(), ""));
            info.setRemoteAddr(StringUtils.defaultIfEmpty(request.getRemoteAddr(), ""));
            info.setRemoteHost(StringUtils.defaultIfEmpty(request.getRemoteHost(), ""));
            info.setRemotePort(request.getRemotePort());
            info.setRemoteUser(StringUtils.defaultIfEmpty(request.getRemoteUser(), ""));
            info.setRequestMethod(StringUtils.defaultIfEmpty(request.getMethod(), ""));
            info.setScheme(StringUtils.defaultIfEmpty(request.getScheme(), ""));
            info.setServerName(StringUtils.defaultIfEmpty(request.getServerName(), ""));
            info.setServerPort(request.getServerPort());
            info.setServletPath(StringUtils.defaultIfEmpty(request.getServletPath(), ""));

            info.setSessionId(StringUtils.defaultIfEmpty(request.getRequestedSessionId(), ""));

            requestInformation.setInformational(info);
        }

        return requestInformation;
    }

    public Headers getHeaders() {
        if (headers == null) {
            headers = OBJECT_FACTORY.createHeaders();
            headers.setRequest(getHeaderList(new RequestHeaderContainer(request)));
            headers.setResponse(getHeaderList(new ResponseHeaderContainer(response)));
        }

        return headers;
    }

    public QueryParameters getRequestParameters() {
        if (queryParameters == null) {
            queryParameters = OBJECT_FACTORY.createQueryParameters();

            if (request != null) {
                List<NameValuePair> parameters = queryParameters.getParameter();
                Set<Entry<String, String[]>> params = request.getParameterMap().entrySet();

                for (Entry<String, String[]> entry : params) {
                    for (String value : entry.getValue()) {
                        NameValuePair param = new NameValuePair();
                        param.setName(entry.getKey());
                        param.setValue(value);

                        parameters.add(param);
                    }
                }

            }
        }

        return queryParameters;
    }
}
