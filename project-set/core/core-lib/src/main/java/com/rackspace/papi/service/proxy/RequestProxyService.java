package com.rackspace.papi.service.proxy;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RequestProxyService {

    int proxyRequest(String targetHost, HttpServletRequest request, HttpServletResponse response) throws IOException;
    void setTimeouts(Integer connectionTimeout, Integer readTimeout, Integer proxyThreadPool);
    ServiceClientResponse get(String uri, Map<String, String> headers);
    ServiceClientResponse get(String baseUri, String extraUri, Map<String, String> headers);
    ServiceClientResponse delete(String baseUri, String extraUri, Map<String, String> headers);
    ServiceClientResponse put(String uri, Map<String, String> headers, byte[] body);
    ServiceClientResponse put(String baseUri, String path, Map<String, String> headers, byte[] body);
    /*
    ServiceClientResponse get(String uri, Map<String, String> headers, String... queryParameters);
    ServiceClientResponse get(String uri, Map<String, String> headers);
    ServiceClientResponse post(String uri, Map<String, String> headers, JAXBElement body, MediaType contentType);
    ServiceClientResponse post(String uri, Map<String, String> headers, byte[] body, MediaType contentType);
    ServiceClientResponse put(String uri, Map<String, String> headers, JAXBElement body, MediaType contentType);
    ServiceClientResponse put(String uri, Map<String, String> headers, byte[] body, MediaType contentType);
    ServiceClientResponse delete(String uri, Map<String, String> headers, String... queryParameters);
    ServiceClientResponse delete(String uri, Map<String, String> headers);
    * 
    */
}
