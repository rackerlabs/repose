package org.openrepose.core.valve.jetty.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ControllerServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerServlet.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.warn("Controller Server Not Yet Implemented");

        resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Controller Server Not Yet Implemented");
    }
}
