package org.openrepose.filters.translation.httpx.parser;

import com.rackspace.httpx.MessageDetail;
import com.rackspace.httpx.MessageEnvelope;
import com.rackspace.httpx.ResponseHeadDetail;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;
import org.openrepose.filters.translation.httpx.marshaller.MarshallerFactory;
import org.openrepose.filters.translation.httpx.node.ComplexNode;
import org.openrepose.filters.translation.httpx.node.ResponseNode;

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
