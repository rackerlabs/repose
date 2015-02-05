package org.openrepose.powerfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class EmptyServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(EmptyServlet.class);

    /**
     * Have to override service so that the EmptyServlet doesn't actually do anything.
     *
     * We could totally probably use Jetty's ProxyServlet in here to make the request instead of doing whatever,
     * This is the last thing called in the container config chain
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.debug("Hit the empty servlet at the end of the chain");
        //Don't actually call super.service, we don't want to do *anything* in here.
    }
}
