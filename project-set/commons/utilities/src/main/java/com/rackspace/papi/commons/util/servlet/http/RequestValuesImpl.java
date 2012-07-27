package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.servlet.http.RequestDestinations;
import com.rackspace.papi.commons.util.servlet.http.RequestHeaderValues;
import com.rackspace.papi.commons.util.servlet.http.RequestQueryParameters;
import com.rackspace.papi.commons.util.servlet.http.RequestValues;
import com.rackspace.papi.commons.util.servlet.http.RequestDestinationsImpl;
import com.rackspace.papi.commons.util.servlet.http.RequestHeaderValuesImpl;
import com.rackspace.papi.commons.util.servlet.http.RequestQueryParametersImpl;
import javax.servlet.http.HttpServletRequest;

public class RequestValuesImpl implements RequestValues {

    private static final String REQUEST_URI_ATTRIBUTE = "repose.request.uri";
    private static final String REQUEST_URL_ATTRIBUTE = "repose.request.url";
    private final HttpServletRequest request;
    private final RequestHeaderValues headers;
    private final RequestDestinations destinations;
    private final RequestQueryParameters parameters;

    public RequestValuesImpl(HttpServletRequest request) {
        this.request = request;
        this.headers = RequestHeaderValuesImpl.extract(request);
        this.destinations = new RequestDestinationsImpl(request);
        this.parameters = new RequestQueryParametersImpl(request);
        request.setAttribute(REQUEST_URI_ATTRIBUTE, request.getRequestURI());
        request.setAttribute(REQUEST_URL_ATTRIBUTE, request.getRequestURL());
    }

    @Override
    public String getRequestURI() {
        return (String) request.getAttribute(REQUEST_URI_ATTRIBUTE);
    }

    @Override
    public void setRequestURI(String uri) {
        request.setAttribute(REQUEST_URI_ATTRIBUTE, uri);
    }

    @Override
    public StringBuffer getRequestURL() {
        return (StringBuffer) request.getAttribute(REQUEST_URL_ATTRIBUTE);
    }

    @Override
    public void setRequestURL(StringBuffer url) {
        request.setAttribute(REQUEST_URL_ATTRIBUTE, url);
    }

    @Override
    public RequestDestinations getDestinations() {
        return destinations;
    }

    @Override
    public RequestHeaderValues getHeaders() {
        return headers;
    }

    @Override
    public RequestQueryParameters getQueryParameters() {
        return parameters;
    }
}
