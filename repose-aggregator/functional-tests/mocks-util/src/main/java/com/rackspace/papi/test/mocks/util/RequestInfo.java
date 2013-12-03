package com.rackspace.papi.test.mocks.util;

import com.rackspace.papi.test.mocks.NameValuePair;
import com.rackspace.papi.test.mocks.RequestInformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestInfo {

    private String url, path, method, queryString, body;
    Map<String, List<String>> headers, queryParams;


    public RequestInfo(String url, String path, String method, String queryString, String body,
                       Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
        this.url = url;
        this.path = path;
        this.method = method;
        this.queryString = queryString;
        this.body = body;

        this.headers = HeaderMap.wrap(headers);
        this.queryParams = queryParams;
    }

    public RequestInfo(RequestInformation requestInformation) {

        this.url = requestInformation.getUri();
        this.path = requestInformation.getPath();
        this.body = requestInformation.getBody();
        this.queryString = requestInformation.getQueryString();
        this.method = requestInformation.getMethod();
        this.headers = requestInformation.getHeaders() != null ?
                HeaderMap.wrap(parseNameValuePair(requestInformation.getHeaders().getHeader())) : new HeaderMap();
        this.queryParams = requestInformation.getQueryParams() != null ?
                parseNameValuePair(requestInformation.getQueryParams().getParameter()) : new HashMap<String, List<String>>();
    }

    private Map<String, List<String>> parseNameValuePair(List<NameValuePair> nameValuePairs) {
        Map<String, List<String>> parsedMap = new HashMap<String, List<String>>();
        for (NameValuePair nvp : nameValuePairs) {

            if (!parsedMap.containsKey(nvp.getName())) {
                parsedMap.put(nvp.getName(), new ArrayList<String>());
            }
            parsedMap.get(nvp.getName()).add(nvp.getValue());
        }
        return parsedMap;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public String getQueryString() {
        return queryString;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }
}
