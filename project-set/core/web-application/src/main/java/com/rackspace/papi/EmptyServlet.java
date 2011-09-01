package com.rackspace.papi;

import com.rackspace.papi.commons.util.StringUtilities;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

public final class EmptyServlet extends HttpServlet {

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        route(req, res);
    }

    private void route(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        final String uriPath = ((HttpServletRequest) servletRequest).getRequestURI();

        if (!StringUtilities.isBlank(uriPath)) {
            final ServletContext targetContext = getServletContext().getContext(uriPath);

            if (targetContext != null) {
                final RequestDispatcher dispatcher = targetContext.getRequestDispatcher(uriPath);
                dispatcher.forward(servletRequest, servletResponse);
            }
        }
    }
}
