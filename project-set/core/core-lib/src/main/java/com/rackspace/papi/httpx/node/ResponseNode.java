package com.rackspace.papi.httpx.node;

import com.rackspace.httpx.MessageDetail;
import com.rackspace.httpx.Response;

import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class ResponseNode implements ComplexNode {
    private final List<Node> nodes = new ArrayList<Node>();
    private final HttpServletResponse response;
    private final Response messageResponse;
    private final List<MessageDetail> responseFidelity;

    public ResponseNode(HttpServletResponse response, Response messageResponse, List<MessageDetail> responseFidelity) {
        this.response = response;
        this.messageResponse = messageResponse;
        this.responseFidelity = responseFidelity;
    }

    @Override
    public void build() {
        messageResponse.setStatusCode(BigInteger.valueOf(response.getStatus()));
        messageResponse.setVersion("HTTP/1.1");
        messageResponse.getFidelity().addAll(responseFidelity);

        for (Node node : nodes) {
            node.build();
        }
    }

    @Override
    public void addChildNode(Node node) {
        nodes.add(node);
    }
}
