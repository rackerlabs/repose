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

import org.openrepose.core.httpx.MessageDetail;
import org.openrepose.core.httpx.MessageEnvelope;
import org.openrepose.core.httpx.Response;
import org.openrepose.core.httpx.ResponseHeadDetail;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class ResponseNode extends ObjectFactoryUser implements ComplexNode {
    private final List<Node> nodes = new ArrayList<Node>();
    private final HttpServletResponse response;
    private final MessageEnvelope messageEnvelope;
    private final List<MessageDetail> responseFidelity;
    private final List<ResponseHeadDetail> headFidelity;
    private final List<String> headersFidelity;
    private final boolean jsonProcessing;

    public ResponseNode(HttpServletResponse response, MessageEnvelope messageEnvelope, List<MessageDetail> responseFidelity, List<ResponseHeadDetail> headFidelity, List<String> headersFidelity, boolean jsonProcessing) {
        this.response = response;
        this.messageEnvelope = messageEnvelope;
        this.responseFidelity = responseFidelity;
        this.headFidelity = headFidelity;
        this.headersFidelity = headersFidelity;
        this.jsonProcessing = jsonProcessing;
    }

    @Override
    public void build() {
        Response messageResponse = getObjectFactory().createResponse();

        messageResponse.setStatusCode(BigInteger.valueOf(response.getStatus()));
        messageResponse.setVersion("HTTP/1.1");
        messageResponse.getFidelity().addAll(responseFidelity);

        for (MessageDetail fidelity : responseFidelity) {
            switch (fidelity) {
                case HEAD:
                    this.addChildNode(new ResponseHeadNode(response, messageResponse, headFidelity, headersFidelity));
                    break;
                case BODY:
                    this.addChildNode(new ResponseBodyNode(response, messageResponse, jsonProcessing));
                    break;
            }
        }

        for (Node node : nodes) {
            node.build();
        }

        messageEnvelope.setResponse(messageResponse);
    }

    @Override
    public void addChildNode(Node node) {
        nodes.add(node);
    }
}
