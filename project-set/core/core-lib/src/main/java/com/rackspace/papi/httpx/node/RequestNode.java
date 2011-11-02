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
    private final Request messageRequest;
    private final List<MessageDetail> requestFidelity;

    public RequestNode(HttpServletRequest request, Request messageRequest, List<MessageDetail> requestFidelity) {
        this.request = request;
        this.messageRequest = messageRequest;
        this.requestFidelity = requestFidelity;
    }

    @Override
    public void build() {
        Method method = Method.fromValue(request.getMethod());

        messageRequest.setMethod(method);
        messageRequest.setUri(request.getRequestURI());
        messageRequest.setVersion(request.getProtocol());

        messageRequest.getFidelity().addAll(requestFidelity);

        for (Node node : nodes) {
            node.build();
        }
    }

    public void addChildNode(Node node) {
        nodes.add(node);
    }
}
