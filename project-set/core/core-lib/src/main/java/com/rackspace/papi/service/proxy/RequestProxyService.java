package com.rackspace.papi.service.proxy;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

public interface RequestProxyService {

    int proxyRequest(String targetHost, HttpServletRequest request, HttpServletResponse response) throws IOException;
    void setTimeouts(Integer connectionTimeout, Integer readTimeout);
    ServiceClientResponse get(String uri, Map<String, String> headers, String... queryParameters);
    ServiceClientResponse post(String uri, JAXBElement body, MediaType contentType);
    ServiceClientResponse post(String uri, byte[] body, MediaType contentType);
    ServiceClientResponse put(String uri, JAXBElement body, MediaType contentType);
    ServiceClientResponse put(String uri, byte[] body, MediaType contentType);
    ServiceClientResponse delete(String uri, Map<String, String> headers, String... queryParameters);
}
