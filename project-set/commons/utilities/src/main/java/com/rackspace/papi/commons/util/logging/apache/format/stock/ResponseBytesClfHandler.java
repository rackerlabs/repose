package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResponseBytesClfHandler implements FormatterLogic {
    private static final String NO_DATA = "-";

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        MutableHttpServletResponse mutableResponse = MutableHttpServletResponse.wrap(request, response);
        long size = mutableResponse.getResponseSize();
        return size == 0? NO_DATA: String.valueOf(size);
    }
}
