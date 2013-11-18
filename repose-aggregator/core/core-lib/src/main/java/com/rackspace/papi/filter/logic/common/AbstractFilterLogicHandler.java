package com.rackspace.papi.filter.logic.common;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.FilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.SimplePassFilterDirector;

import javax.servlet.http.HttpServletRequest;

public class AbstractFilterLogicHandler implements FilterLogicHandler {

    @Override
    // TODO: Remove the response object from this method
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        return SimplePassFilterDirector.getInstance();
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
        return SimplePassFilterDirector.getInstance();
    }
}
