package org.openrepose.core.valve.jetty.servlet;

import org.openrepose.core.services.context.impl.PowerApiContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 * @author zinic
 */
public class ProxyServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyServlet.class);
    private boolean initialized = false;

    public ProxyServlet() {
    }

    private boolean isRequestFilterChainComplete(HttpServletRequest req) {
       return (Boolean)req.getAttribute("filterChainAvailableForRequest");
    }

    private boolean isPowerApiContextManagerIntiliazed() {
        if (initialized) {
            return true;
        }

        PowerApiContextManager manager = (PowerApiContextManager) getServletContext().getAttribute("powerApiContextManager");

        if (manager == null || !manager.isContextInitialized()) {
            return false;
        }

        initialized = true;

        return initialized;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isPowerApiContextManagerIntiliazed() || !isRequestFilterChainComplete(req)) {
            LOG.debug("Filter chain is not available to process request.");
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Filter chain is not available to process request");
        }
    }
}
