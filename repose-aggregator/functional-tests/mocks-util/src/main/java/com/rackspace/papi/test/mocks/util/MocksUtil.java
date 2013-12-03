package com.rackspace.papi.test.mocks.util;

import com.rackspace.papi.test.mocks.*;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.*;
import java.io.*;
import java.util.Arrays;
import java.util.Enumeration;

/*
    Marshalling/Unmarshalling utility for request info
 */
public final class MocksUtil {

    public static final String CONTEXT_PATH = "com.rackspace.papi.test.mocks";

    private MocksUtil() {
    }

    public static RequestInformation xmlStreamToRequestInformation(InputStream inputStream) throws JAXBException {

        JAXBContext jaxbContext = JAXBContext.newInstance(CONTEXT_PATH);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        return ((JAXBElement<RequestInformation>) unmarshaller.unmarshal(inputStream)).getValue();
    }

    public static RequestInformation xmlStringToRequestInformation(String request) throws JAXBException {
        InputStream is = new ByteArrayInputStream(request.getBytes());
        return xmlStreamToRequestInformation(is);
    }

    public static String requestInformationToXml(RequestInformation requestInformation) throws JAXBException {

        ObjectFactory factory = new ObjectFactory();
        JAXBContext jaxbContext = JAXBContext.newInstance(CONTEXT_PATH);
        Marshaller marshaller = jaxbContext.createMarshaller();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaller.marshal(factory.createRequestInfo(requestInformation), baos);

        return baos.toString();
    }

    public static RequestInformation servletRequestToRequestInformation(HttpServletRequest request) throws IOException {

        String body = "";
        if (request.getInputStream() != null) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(request.getInputStream(), writer, "UTF-8");
            body = writer.toString();
        }

        return servletRequestToRequestInformation(request, body);

    }

    public static RequestInformation servletRequestToRequestInformation(HttpServletRequest request, String body) throws IOException {

        RequestInformation req = new RequestInformation();

        req.setUri(request.getRequestURL().toString());
        req.setPath(request.getRequestURI());
        req.setMethod(request.getMethod());
        req.setQueryString(request.getQueryString());
        req.setBody(body);

        if (!request.getParameterMap().isEmpty()) {
            QueryParameters q = new QueryParameters();
            Enumeration<String> queryParamNames = request.getParameterNames();
            while (queryParamNames.hasMoreElements()) {
                String name = queryParamNames.nextElement();
                String value = Arrays.toString(request.getParameterMap().get(name));
                NameValuePair nvp = new NameValuePair();
                nvp.setName(name);
                nvp.setValue(value);
                q.getParameter().add(nvp);
            }
            req.setQueryParams(q);
        }

        HeaderList h = new HeaderList();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                NameValuePair nvp = new NameValuePair();
                nvp.setName(headerName);
                nvp.setValue(headerValue);
                h.getHeader().add(nvp);
            }
        }
        req.setHeaders(h);
        return req;
    }

    public static RequestInfo xmlStringToRequestInfo(String xml) throws JAXBException {

        return new RequestInfo(xmlStringToRequestInformation(xml));
    }

    public static String getServletPath(String filePath){

        int dot = filePath.lastIndexOf(".");
        int slash = filePath.lastIndexOf("/");

        return filePath.substring(slash+1, dot);

    }
}
