package com.rackspace.papi.httpx.node;

import com.rackspace.httpx.Request;
import com.rackspace.httpx.RequestHead;
import com.rackspace.httpx.RequestHeadDetail;
import com.rackspace.papi.httpx.ObjectFactoryUser;

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
