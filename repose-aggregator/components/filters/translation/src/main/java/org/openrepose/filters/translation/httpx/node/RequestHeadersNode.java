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
package org.openrepose.filters.translation.httpx.node;

import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.core.httpx.ComplexParameter;
import org.openrepose.core.httpx.RequestHead;
import org.openrepose.core.httpx.RequestHeaders;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author fran
 */
public class RequestHeadersNode extends ObjectFactoryUser implements Node {
    private final HttpServletRequest request;
    private final RequestHead requestHead;
    private final List<String> fidelity;
    private final AcceptFidelityValidator fidelityValidator;

    public RequestHeadersNode(HttpServletRequest request, RequestHead requestHead, List<String> fidelity) {
        this.request = request;
        this.requestHead = requestHead;
        this.fidelity = fidelity;
        this.fidelityValidator = new AcceptFidelityValidator(fidelity);
    }

    @Override
    public void build() {
        RequestHeaders requestHeaders = getObjectFactory().createRequestHeaders();

        requestHeaders.getFidelity().addAll(fidelity);

        if (fidelityValidator.hasValidFidelity()) {
            while (request.getHeaderNames().hasMoreElements()) {

                String headerName = request.getHeaderNames().nextElement();

                if (CommonHttpHeader.ACCEPT.matches(headerName) && fidelityValidator.hasAcceptFidelity()) {
                    new AcceptHeaderNode(request.getHeader(CommonHttpHeader.ACCEPT.toString()), requestHeaders).build();
                } else if (fidelityValidator.hasStarFidelity()) {
                    ComplexParameter complexParameter = getObjectFactory().createComplexParameter();
                    complexParameter.setName(headerName);

                    while (request.getHeaders(headerName).hasMoreElements()) {
                        String nextElement = request.getHeaders(headerName).nextElement();

                        complexParameter.getValue().add(nextElement);
                    }

                    requestHeaders.getHeader().add(complexParameter);
                }
            }
        }

        requestHead.setHeaders(requestHeaders);
    }
}
