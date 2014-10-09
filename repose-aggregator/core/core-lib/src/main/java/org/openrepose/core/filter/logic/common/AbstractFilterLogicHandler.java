package org.openrepose.core.filter.logic.common;

import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.FilterLogicHandler;
import org.openrepose.core.filter.logic.impl.SimplePassFilterDirector;

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
