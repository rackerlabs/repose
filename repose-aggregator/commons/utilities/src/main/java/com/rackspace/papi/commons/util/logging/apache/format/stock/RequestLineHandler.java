package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestLineHandler implements FormatterLogic {
    private static final char SPACE = ' ';

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        return new StringBuilder(request.getMethod()).append(SPACE)
                .append(request.getRequestURI()).append(SPACE)
                .append(request.getProtocol()).toString();
    }
}

