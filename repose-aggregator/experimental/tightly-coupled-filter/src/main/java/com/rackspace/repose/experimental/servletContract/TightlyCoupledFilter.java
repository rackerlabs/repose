package com.rackspace.repose.experimental.servletContract;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Scanner;

/**
 * This experimental filter is to verify that repose can do modifications of the response. Even if it's tightly coupled.
 */
public class TightlyCoupledFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(TightlyCoupledFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        LOG.debug("Start " + this.getClass().getName());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        //Use the repose internal Wrapper to grab a response and modify it
        MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap((HttpServletRequest) servletRequest);
        mutableRequest.setInputStream(servletRequest.getInputStream());

        //Use a repose internal mutable response
        MutableHttpServletResponse mutableResponse = MutableHttpServletResponse.wrap((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);

        //Fire off the next one in the filter chain
        filterChain.doFilter(mutableRequest, mutableResponse);

        HttpServletRequest req = (HttpServletRequest) servletRequest;

        // Print out info from request & response wrapper
        LOG.debug("URI: " + req.getRequestURI());
        LOG.debug("Status: " + mutableResponse.getStatus());
        //I don't know why this doesn't work :(
        LOG.debug("mutable content-type: " + mutableResponse.getContentType());
        //I don't know why this doesn't work either :(
        LOG.debug("regular content-type: " + servletRequest.getContentType());
        LOG.debug("resp Header 'Content-Type: " + mutableResponse.getHeader("Content-Type"));
        //THis is not reliable either. You'll have to check the body proper by getting the input stream and reading it
        LOG.debug("Has body: " + mutableResponse.hasBody());

        //Just a scanner to read in the entire content
        String content = "";
        Scanner s = new Scanner(mutableResponse.getInputStream()).useDelimiter("\\A");
        if (s.hasNext()) {
            content = s.next();
        }

        LOG.debug("Content Body: '" + content + "'");

        // verify that the content is not empty.
        if (content.isEmpty()) {
            throw new RuntimeException("Content is empty");
        }

        //Make the changes to the body you want to do here, then commit it.
        mutableResponse.getWriter().write(content + "<extra> Added by TestFilter, should also see the rest of the content </extra>");
        mutableResponse.commitBufferToServletOutputStream(); //THIS MUST BE CALLED HERE TO GET THE THINGS INTO THE BODY
    }

    @Override
    public void destroy() {
    }
}
