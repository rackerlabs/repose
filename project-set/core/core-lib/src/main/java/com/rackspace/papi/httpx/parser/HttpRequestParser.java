package com.rackspace.papi.httpx.parser;

import com.rackspace.httpx.MessageDetail;
import com.rackspace.httpx.MessageEnvelope;
import com.rackspace.httpx.RequestHeadDetail;
import com.rackspace.papi.httpx.ObjectFactoryUser;
import com.rackspace.papi.httpx.marshaller.MarshallerFactory;
import com.rackspace.papi.httpx.node.ComplexNode;
import com.rackspace.papi.httpx.node.RequestNode;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.List;

/**
 * @author fran
 */
public class HttpRequestParser extends ObjectFactoryUser implements Parser<HttpServletRequest, RequestHeadDetail> {

    @Override
    public InputStream parse(HttpServletRequest request, List<MessageDetail> requestFidelity, List<RequestHeadDetail> headFidelity, List<String> headersFidelity, boolean jsonProcessing) {
        MessageEnvelope messageEnvelope = getObjectFactory().createMessageEnvelope();

        ComplexNode requestNode = new RequestNode(request, messageEnvelope, requestFidelity, headFidelity, headersFidelity, jsonProcessing);

        requestNode.build();
        
        return MarshallerFactory.newInstance().marshall(messageEnvelope);
    }
}