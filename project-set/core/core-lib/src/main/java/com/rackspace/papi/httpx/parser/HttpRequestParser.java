package com.rackspace.papi.httpx.parser;

import com.rackspace.httpx.*;
import com.rackspace.papi.httpx.ObjectFactoryUser;
import com.rackspace.papi.httpx.marshaller.MarshallerFactory;
import com.rackspace.papi.httpx.node.*;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.List;

/**
 * @author fran
 */
public class HttpRequestParser extends ObjectFactoryUser implements Parser<HttpServletRequest> {

    @Override
    public InputStream parse(HttpServletRequest request, List<MessageDetail> requestFidelity, List<RequestHeadDetail> headFidelity, List<String> headersFidelity) {

        MessageEnvelope messageEnvelope = objectFactory.createMessageEnvelope();
        Request messageRequest = objectFactory.createRequest();

        ComplexNode requestNode = new RequestNode(request, messageRequest, requestFidelity);

        for (MessageDetail fidelity : requestFidelity) {
            switch (fidelity) {
                case HEAD:
                    RequestHead head = objectFactory.createRequestHead();
                    ComplexNode headNode = new HeadNode(messageRequest, head, headFidelity);

                    for (RequestHeadDetail headDetail : headFidelity) {

                        switch (headDetail) {
                            case URI_DETAIL:
                                headNode.addChildNode(new UriDetailNode(request.getParameterMap(), head));
                                break;
                            case HEADERS:
                                headNode.addChildNode(new HeadersNode(request, head, headersFidelity));
                        }
                    }

                    requestNode.addChildNode(headNode);
                    break;
            }
        }

        // Unless we want the parser to merge streams, for now we always set an empty body tag
        Body body = objectFactory.createBody();
        messageRequest.setBody(body);

        requestNode.build();
        
        messageEnvelope.setRequest(messageRequest);

        return MarshallerFactory.newInstance().marshall(messageEnvelope);
    }
}