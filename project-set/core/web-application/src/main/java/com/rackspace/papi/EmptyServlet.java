package com.rackspace.papi;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;

public final class EmptyServlet extends HttpServlet {

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
    }
}
