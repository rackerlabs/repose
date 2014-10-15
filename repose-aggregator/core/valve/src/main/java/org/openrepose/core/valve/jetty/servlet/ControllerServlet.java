package org.openrepose.core.valve.jetty.servlet;

import org.openrepose.commons.utils.http.HttpStatusCode;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerServlet.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.warn("Controller Server Not Yet Implemented");

        resp.sendError(HttpStatusCode.NOT_IMPLEMENTED.intValue(), "Controller Server Not Yet Implemented");
    }
}
