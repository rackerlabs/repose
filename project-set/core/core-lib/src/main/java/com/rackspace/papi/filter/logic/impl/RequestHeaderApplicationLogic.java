package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.filter.logic.HeaderApplicationLogic;

import java.util.Set;

public class RequestHeaderApplicationLogic implements HeaderApplicationLogic {

    final MutableHttpServletRequest request;

    public RequestHeaderApplicationLogic(final MutableHttpServletRequest request) {
        this.request = request;
    }

    @Override
    public void removeHeader(String headerName) {
        request.removeHeader(headerName);
    }

    @Override
    public void addHeader(String key, Set<String> values) {
//        request.removeHeader(key);

        for (String value : values) {
            request.addHeader(key, value);
        }
    }
}
