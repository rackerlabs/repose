package features.filters.translation.httpx.parser;

import com.rackspace.httpx.MessageDetail;
import com.rackspace.httpx.MessageEnvelope;
import com.rackspace.httpx.RequestHeadDetail;
import features.filters.translation.httpx.ObjectFactoryUser;
import features.filters.translation.httpx.marshaller.MarshallerFactory;
import features.filters.translation.httpx.node.ComplexNode;
import features.filters.translation.httpx.node.RequestNode;

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