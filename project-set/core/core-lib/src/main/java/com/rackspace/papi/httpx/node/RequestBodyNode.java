package com.rackspace.papi.httpx.node;

import com.rackspace.httpx.Body;
import com.rackspace.httpx.Request;
import com.rackspace.papi.httpx.ObjectFactoryUser;
import com.rackspace.papi.httpx.processor.TranslationRequestPreProcessor;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author fran
 */
public class RequestBodyNode extends ObjectFactoryUser implements Node {
    private final HttpServletRequest request;
    private final Request messageRequest;
    private final boolean jsonProcessing;

    public RequestBodyNode(HttpServletRequest request, Request messageRequest, boolean jsonProcessing) {
        this.request = request;
        this.messageRequest = messageRequest;
        this.jsonProcessing = jsonProcessing;
    }

    @Override
    public void build() {
        Body body = getObjectFactory().createBody();

        TranslationRequestPreProcessor processor = new TranslationRequestPreProcessor(request, jsonProcessing);

        try {
            body.getContent().add(processor.getBodyStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        messageRequest.setBody(body);
    }
}
