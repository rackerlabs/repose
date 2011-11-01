package com.rackspace.papi.httpx.parser;

import com.rackspace.httpx.*;
import com.rackspace.papi.httpx.ObjectFactoryUser;
import com.rackspace.papi.httpx.marshaller.MarshallerFactory;
import com.rackspace.papi.httpx.node.HeadNode;
import com.rackspace.papi.httpx.node.HeadersNode;
import com.rackspace.papi.httpx.node.UriDetailNode;

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
        Method method = Method.fromValue(request.getMethod());

        messageRequest.setMethod(method);
        messageRequest.setUri(request.getRequestURI());
        messageRequest.setVersion(request.getProtocol());

        // TODO: Play with the algorithm here to clean up nesting
        for (MessageDetail detail : requestFidelity) {
            messageRequest.getFidelity().add(detail);

            switch (detail) {

                case HEAD: {
                    RequestHead head = objectFactory.createRequestHead();
                    HeadNode headNode = new HeadNode(messageRequest, head, headFidelity);
                    
                    for (RequestHeadDetail headDetail : headFidelity) {

                        switch (headDetail) {
                            case URI_DETAIL: {
                                headNode.addChildNode(new UriDetailNode(request.getParameterMap(), head));
                            }
                            break;
                            case HEADERS: {
                                headNode.addChildNode(new HeadersNode(request, head, headersFidelity));
                            }
                        }
                    }

                    headNode.build();
                }
                break;
                default:
                    Body body = objectFactory.createBody();                 
                    messageRequest.setBody(body);
            }
        }

        messageEnvelope.setRequest(messageRequest);

        return MarshallerFactory.newInstance().marshall(messageEnvelope);
    }
}