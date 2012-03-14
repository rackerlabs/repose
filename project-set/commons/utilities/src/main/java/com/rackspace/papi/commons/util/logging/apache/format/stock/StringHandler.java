package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StringHandler implements FormatterLogic {

    private final String staticStringContent;

    public StringHandler(String staticStringContent) {
        this.staticStringContent = staticStringContent;
    }

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        return staticStringContent;
    }
}
