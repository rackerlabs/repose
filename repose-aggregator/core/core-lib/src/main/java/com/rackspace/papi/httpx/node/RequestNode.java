package com.rackspace.papi.httpx.node;

import com.rackspace.httpx.*;
import com.rackspace.papi.httpx.ObjectFactoryUser;

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
                case BODY :
                    this.addChildNode(new RequestBodyNode(request, messageRequest, jsonProcessing));
            }
        }

        for (Node node : nodes) {
            node.build();
        }

        messageEnvelope.setRequest(messageRequest);
    }

    public void addChildNode(Node node) {
        nodes.add(node);
    }
}
