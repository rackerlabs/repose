package com.rackspace.papi.httpx.node;

import com.rackspace.httpx.ComplexParameter;
import com.rackspace.httpx.RequestHead;
import com.rackspace.httpx.RequestHeaders;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.httpx.ObjectFactoryUser;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author fran
 */
public class HeadersNode extends ObjectFactoryUser implements Node {
    private final HttpServletRequest request;
    private final RequestHead requestHead;
    private final List<String> fidelity;
    private final AcceptFidelityValidator fidelityValidator;

    public HeadersNode(HttpServletRequest request, RequestHead requestHead, List<String> fidelity) {
        this.request = request;
        this.requestHead = requestHead;
        this.fidelity = fidelity;
        this.fidelityValidator = new AcceptFidelityValidator(fidelity);
    }

    @Override
    public void build() {
        RequestHeaders requestHeaders = objectFactory.createRequestHeaders();

        requestHeaders.getFidelity().addAll(fidelity);        

        if (fidelityValidator.hasValidFidelity()) {
            while (request.getHeaderNames().hasMoreElements()) {

                String headerName = request.getHeaderNames().nextElement();

                if (headerName.equalsIgnoreCase(CommonHttpHeader.ACCEPT.headerKey()) && fidelityValidator.hasAcceptFidelity()) {
                    new AcceptHeaderNode(request.getHeader(CommonHttpHeader.ACCEPT.headerKey()), requestHeaders).build();
                } else if (fidelityValidator.hasStarFidelity()){
                    ComplexParameter complexParameter = objectFactory.createComplexParameter();
                    complexParameter.setName(headerName);

                    while (request.getHeaders(headerName).hasMoreElements()) {
                        String nextElement = request.getHeaders(headerName).nextElement();

                        complexParameter.getValue().add(nextElement);
                    }

                    requestHeaders.getHeader().add(complexParameter);
                }
            }
        }

        requestHead.setHeaders(requestHeaders);
    }
}
