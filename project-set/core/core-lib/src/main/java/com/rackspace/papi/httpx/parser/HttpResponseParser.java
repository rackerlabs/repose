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
    public InputStream parse(HttpServletResponse response, List<MessageDetail> responseFidelity, List<ResponseHeadDetail> headFidelity, List<String> headersFidelity, boolean jsonProcessing) {
        MessageEnvelope messageEnvelope = getObjectFactory().createMessageEnvelope();

        ComplexNode responseNode = new ResponseNode(response, messageEnvelope, responseFidelity, headFidelity, headersFidelity, jsonProcessing);

        responseNode.build();

        return MarshallerFactory.newInstance().marshall(messageEnvelope);
    }
}
