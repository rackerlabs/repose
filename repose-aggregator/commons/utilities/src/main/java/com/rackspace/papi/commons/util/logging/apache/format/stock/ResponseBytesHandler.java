package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResponseBytesHandler implements FormatterLogic {

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        MutableHttpServletResponse mutableResponse = MutableHttpServletResponse.wrap(request, response);
        return String.valueOf(mutableResponse.getResponseSize());
    }
}
