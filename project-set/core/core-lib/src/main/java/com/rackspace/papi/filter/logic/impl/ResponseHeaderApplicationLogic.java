package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.filter.logic.HeaderApplicationLogic;

import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public class ResponseHeaderApplicationLogic implements HeaderApplicationLogic {

    final HttpServletResponse response;

    public ResponseHeaderApplicationLogic(final HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void removeHeader(String headerName) {
        throw new UnsupportedOperationException("Responses do not support header removal");
    }

    @Override
    public void addHeader(String key, Set<String> values) {
        for (String value : values) {
            response.addHeader(key, value);
        }
    }
}
