package org.openrepose.filters.translation.httpx.node;

import org.openrepose.core.httpx.ComplexParameter;
import org.openrepose.core.httpx.ResponseHead;
import org.openrepose.core.httpx.ResponseHeaders;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author fran
 */
public class ResponseHeadersNode extends ObjectFactoryUser implements Node {
    private final HttpServletResponse response;
    private final ResponseHead responseHead;
    private final List<String> fidelity;
    private final AcceptFidelityValidator fidelityValidator;

    public ResponseHeadersNode(HttpServletResponse response, ResponseHead responseHead, List<String> fidelity) {
        this.response = response;
        this.responseHead = responseHead;
        this.fidelity = fidelity;
        this.fidelityValidator = new AcceptFidelityValidator(fidelity);
    }

    @Override
    public void build() {
        ResponseHeaders responseHeaders = getObjectFactory().createResponseHeaders();

        responseHeaders.getFidelity().addAll(fidelity);

        if (fidelityValidator.hasValidFidelity()) {

            for (String headerName : response.getHeaderNames()) {

                if (fidelityValidator.hasStarFidelity()){
                    ComplexParameter complexParameter = getObjectFactory().createComplexParameter();
                    complexParameter.setName(headerName);

                    for (String nextElement : response.getHeaders(headerName)) {
                        complexParameter.getValue().add(nextElement);
                    }

                    responseHeaders.getHeader().add(complexParameter);
                }
            }
        }

        responseHead.setHeaders(responseHeaders);
    }
}
