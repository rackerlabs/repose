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

import org.openrepose.core.httpx.*;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class RequestNode extends ObjectFactoryUser implements ComplexNode {
    private final List<Node> nodes = new ArrayList<Node>();
    private final HttpServletRequest request;
    private final MessageEnvelope messageEnvelope;
    private final List<MessageDetail> requestFidelity;
    private final List<RequestHeadDetail> headFidelity;
    private final List<String> headersFidelity;
    private final boolean jsonProcessing;

    public RequestNode(HttpServletRequest request, MessageEnvelope messageEnvelope, List<MessageDetail> requestFidelity, List<RequestHeadDetail> headFidelity, List<String> headersFidelity, boolean jsonProcessing) {
        this.request = request;
        this.messageEnvelope = messageEnvelope;
        this.requestFidelity = requestFidelity;
        this.headFidelity = headFidelity;
        this.headersFidelity = headersFidelity;
        this.jsonProcessing = jsonProcessing;
    }

    @Override
    public void build() {
        Request messageRequest = getObjectFactory().createRequest();

        Method method = Method.fromValue(request.getMethod());

        messageRequest.setMethod(method);
        messageRequest.setUri(request.getRequestURI());
        messageRequest.setVersion(request.getProtocol());

        messageRequest.getFidelity().addAll(requestFidelity);

        for (MessageDetail fidelity : requestFidelity) {
            switch (fidelity) {
                case HEAD:
                    this.addChildNode(new RequestHeadNode(request, messageRequest, headFidelity, headersFidelity));
                    break;
                case BODY:
                    this.addChildNode(new RequestBodyNode(request, messageRequest, jsonProcessing));
                    break;
            }
        }

        for (Node node : nodes) {
            node.build();
        }

        messageEnvelope.setRequest(messageRequest);
    }

    @Override
    public void addChildNode(Node node) {
        nodes.add(node);
    }
}
