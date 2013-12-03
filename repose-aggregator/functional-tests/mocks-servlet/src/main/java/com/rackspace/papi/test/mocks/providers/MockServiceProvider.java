package com.rackspace.papi.test.mocks.providers;

import com.rackspace.papi.test.mocks.util.MocksUtil;
import com.rackspace.papi.test.mocks.ObjectFactory;
import com.rackspace.papi.test.mocks.RequestInformation;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBException;
import java.io.IOException;

public class MockServiceProvider {

    private final ObjectFactory factory;

    public MockServiceProvider() {
        factory = new ObjectFactory();
    }

    public String getEchoBody(HttpServletRequest request, String body) {

        StringBuilder resp = new StringBuilder("");
        try {
            RequestInformation requestInformation = MocksUtil.servletRequestToRequestInformation(request, body);
            resp = resp.append(MocksUtil.requestInformationToXml(requestInformation));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException j) {
        }

        return resp.toString();

    }

    public Response getEndService(HttpServletRequest request, String body) {
        return getEndService("200", request, body);
    }

    public Response getEndService(HttpServletRequest request) {
        return getEndService("200", request);
    }

    public Response getEndService(String statusCode, HttpServletRequest request) {

        return getEndService(statusCode, request, "");
    }

    public Response getEndService(String statusCode, HttpServletRequest request, String body) {
        int status;
        try {
            status = Integer.parseInt(statusCode);
        } catch (NumberFormatException e) {

            status = Response.Status.NOT_FOUND.getStatusCode();
        }

        String resp = getEchoBody(request, body);

        ResponseBuilder response = Response.status(status);

        return response.entity(resp).header("x-request-id", "somevalue").header("Content-Length", resp.length()).build();
    }

}
