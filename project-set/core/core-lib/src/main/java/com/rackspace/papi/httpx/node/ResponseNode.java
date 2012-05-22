package com.rackspace.papi.httpx.node;

import com.rackspace.httpx.MessageDetail;
import com.rackspace.httpx.MessageEnvelope;
import com.rackspace.httpx.Response;
import com.rackspace.httpx.ResponseHeadDetail;
import com.rackspace.papi.httpx.ObjectFactoryUser;

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
