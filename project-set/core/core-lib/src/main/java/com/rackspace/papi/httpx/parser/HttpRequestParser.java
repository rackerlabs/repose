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
public class HttpRequestParser extends ObjectFactoryUser implements Parser<HttpServletRequest, RequestHeadDetail> {

    @Override
    public InputStream parse(HttpServletRequest request, List<MessageDetail> requestFidelity, List<RequestHeadDetail> headFidelity, List<String> headersFidelity, boolean jsonProcessing) {
        MessageEnvelope messageEnvelope = objectFactory.createMessageEnvelope();

        ComplexNode requestNode = new RequestNode(request, messageEnvelope, requestFidelity, headFidelity, headersFidelity, jsonProcessing);

        requestNode.build();
        
        return MarshallerFactory.newInstance().marshall(messageEnvelope);
    }
}