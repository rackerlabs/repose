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

import org.openrepose.core.httpx.Request;
import org.openrepose.core.httpx.RequestHead;
import org.openrepose.core.httpx.RequestHeadDetail;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class RequestHeadNode extends ObjectFactoryUser implements ComplexNode {
    private final List<Node> nodes = new ArrayList<Node>();
    private final HttpServletRequest request;
    private final Request message;
    private final List<RequestHeadDetail> headFidelity;
    private final List<String> headersFidelity;

    public RequestHeadNode(HttpServletRequest request, Request message, List<RequestHeadDetail> headFidelity, List<String> headersFidelity) {
        this.request = request;
        this.message = message;
        this.headFidelity = headFidelity;
        this.headersFidelity = headersFidelity;
    }

    @Override
    public void build() {
        RequestHead head = getObjectFactory().createRequestHead();

        head.getFidelity().addAll(headFidelity);

        for (RequestHeadDetail headDetail : headFidelity) {

            switch (headDetail) {
                case URI_DETAIL:
                    this.addChildNode(new UriDetailNode(request.getParameterMap(), head));
                    break;
                case HEADERS:
                    this.addChildNode(new RequestHeadersNode(request, head, headersFidelity));
                    break;
            }
        }

        for (Node node : nodes) {
            node.build();
        }

        message.setHead(head);
    }

    @Override
    public void addChildNode(Node node) {
        nodes.add(node);
    }
}
