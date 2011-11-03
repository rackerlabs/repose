package com.rackspace.papi.httpx.node;

import com.rackspace.httpx.Request;
import com.rackspace.httpx.RequestHead;
import com.rackspace.httpx.RequestHeadDetail;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class RequestHeadNode implements ComplexNode {
    private final List<Node> nodes = new ArrayList<Node>();
    private final Request message;
    private final RequestHead head;
    private final List<RequestHeadDetail> headFidelity;

    public RequestHeadNode(Request message, RequestHead head, List<RequestHeadDetail> headFidelity) {
        this.message = message;
        this.head = head;
        this.headFidelity = headFidelity;
    }

    @Override
    public void build() {
        head.getFidelity().addAll(headFidelity);

        for (Node node : nodes) {
            node.build();
        }

        message.setHead(head);
    }

    public void addChildNode(Node node) {
        nodes.add(node);
    }
}
