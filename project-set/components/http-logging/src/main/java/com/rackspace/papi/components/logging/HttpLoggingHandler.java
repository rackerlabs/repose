package com.rackspace.papi.components.logging;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Dan Daley
 */
public class HttpLoggingHandler extends AbstractFilterLogicHandler {
    private final List<HttpLoggerWrapper> loggers;

    public HttpLoggingHandler(List<HttpLoggerWrapper> loggers) {
        this.loggers = loggers;
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
        FilterDirector filterDirector = new FilterDirectorImpl(HttpStatusCode.fromInt(response.getStatus()), FilterAction.PASS);

        for (HttpLoggerWrapper loggerWrapper : loggers) {
            loggerWrapper.handle(request, response);
        }

        return filterDirector;
    }
}
