package com.rackspace.papi.httpx.parser;

import com.rackspace.httpx.*;
import com.rackspace.papi.httpx.ObjectFactoryUser;
import com.rackspace.papi.httpx.marshaller.MarshallerFactory;
import com.rackspace.papi.httpx.node.*;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

/**
 * @author fran
 */
public class HttpResponseParser extends ObjectFactoryUser implements Parser<HttpServletResponse, ResponseHeadDetail> {

    @Override
    public InputStream parse(HttpServletResponse response, List<MessageDetail> responseFidelity, List<ResponseHeadDetail> headFidelity, List<String> headersFidelity, boolean jsonPreprocessing) {
        MessageEnvelope messageEnvelope = objectFactory.createMessageEnvelope();
        Response messageResponse = objectFactory.createResponse();

        ComplexNode responseNode = new ResponseNode(response, messageResponse, responseFidelity);

        for (MessageDetail fidelity : responseFidelity) {
            switch (fidelity) {
                case HEAD:
                    ResponseHead head = objectFactory.createResponseHead();
                    ComplexNode headNode = new ResponseHeadNode(messageResponse, head, headFidelity);

                    for (ResponseHeadDetail headDetail : headFidelity) {

                        switch (headDetail) {
                            case HEADERS:
                                headNode.addChildNode(new ResponseHeadersNode(response, head, headersFidelity));
                        }
                    }

                    responseNode.addChildNode(headNode);
                    break;
                case BODY:
                    Body body = objectFactory.createBody();
                    messageResponse.setBody(body);
            }
        }        

        responseNode.build();

        messageEnvelope.setResponse(messageResponse);

        return MarshallerFactory.newInstance().marshall(messageEnvelope);
    }
}
