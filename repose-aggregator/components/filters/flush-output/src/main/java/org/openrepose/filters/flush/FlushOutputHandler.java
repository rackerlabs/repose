package org.openrepose.filters.flush;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import java.io.IOException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

public class FlushOutputHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FlushOutputHandler.class);

    public FlushOutputHandler() {
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
        return myDirector;
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
        MutableHttpServletResponse mutableResponse = MutableHttpServletResponse.wrap(request, response);
        try {
            mutableResponse.commitBufferToServletOutputStream();
        } catch (IOException ex) {
            LOG.error("Failed to flush output", ex);
        }

        return new FilterDirectorImpl();
    }
}
