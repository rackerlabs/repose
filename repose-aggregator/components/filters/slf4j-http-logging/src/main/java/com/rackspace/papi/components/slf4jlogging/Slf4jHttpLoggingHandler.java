package com.rackspace.papi.components.slf4jlogging;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class Slf4jHttpLoggingHandler extends AbstractFilterLogicHandler {
    private final List<Slf4jLoggerWrapper> loggers;

    public Slf4jHttpLoggingHandler(List<Slf4jLoggerWrapper> loggers) {
        this.loggers = loggers;
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
        FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setResponseStatusCode(response.getStatus());
        filterDirector.setFilterAction(FilterAction.PASS);

        for (Slf4jLoggerWrapper wrapper : loggers) {
            //format the string and send it to the logger
            //logger.info(formattedOutput);
            throw new UnsupportedOperationException("TODO IMPLEMENT ME");
        }

        return filterDirector;
    }
    
    
    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response){
        FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
        return filterDirector;
        
    }
}
