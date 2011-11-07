package com.rackspace.papi.httpx.node;

import com.rackspace.httpx.*;
import com.rackspace.papi.httpx.ObjectFactoryUser;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class ResponseHeadNode extends ObjectFactoryUser implements ComplexNode {
    private final List<Node> nodes = new ArrayList<Node>();
    private final HttpServletResponse response;
    private final Response message;
    private final List<ResponseHeadDetail> headFidelity;
    private final List<String> headersFidelity;

    public ResponseHeadNode(HttpServletResponse response, Response message, List<ResponseHeadDetail> headFidelity, List<String> headersFidelity) {
        this.response = response;
        this.message = message;
        this.headFidelity = headFidelity;
        this.headersFidelity = headersFidelity;
    }

    @Override
    public void build() {                
        ResponseHead head = objectFactory.createResponseHead();

        head.getFidelity().addAll(headFidelity);

        for (ResponseHeadDetail headDetail : headFidelity) {

            switch (headDetail) {
                case HEADERS:
                    this.addChildNode(new ResponseHeadersNode(response, head, headersFidelity));
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
