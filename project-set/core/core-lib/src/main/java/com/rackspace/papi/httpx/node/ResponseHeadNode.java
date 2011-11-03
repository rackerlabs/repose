package com.rackspace.papi.httpx.node;

import com.rackspace.httpx.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class ResponseHeadNode implements ComplexNode {
    private final List<Node> nodes = new ArrayList<Node>();
    private final Response message;
    private final ResponseHead head;
    private final List<ResponseHeadDetail> headFidelity;

    public ResponseHeadNode(Response message, ResponseHead head, List<ResponseHeadDetail> headFidelity) {
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
