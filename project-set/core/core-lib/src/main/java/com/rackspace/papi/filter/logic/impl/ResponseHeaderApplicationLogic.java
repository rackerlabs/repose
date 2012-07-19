package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.filter.logic.HeaderApplicationLogic;

import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public class ResponseHeaderApplicationLogic implements HeaderApplicationLogic {

    private final HttpServletResponse response;

    public ResponseHeaderApplicationLogic(final HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void removeHeader(String headerName) {
       // NOOP
       // TODO: Log use of this for consideration of reworking the model
        response.setHeader(headerName, null);
    }

    @Override
    public void addHeader(String key, Set<String> values) {
        for (String value : values) {
            response.addHeader(key, value);
        }
    }
}
