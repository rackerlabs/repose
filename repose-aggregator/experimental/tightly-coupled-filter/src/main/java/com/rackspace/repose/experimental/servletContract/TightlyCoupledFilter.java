package com.rackspace.repose.experimental.servletContract;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * This test is to verify that repose supports the contract on the ServletResponse.getOutputStream()
 * and ServletResponse.getWriter() methods.
 * <p/>
 * If I pass a ResponseWrapper along the filter chain and override the getOutputStream() & getWriter() methods, I should
 * be able to access the results written to those methods through the ResponseWrapper.getContent() method, contained
 * in this file.  This isn't the case.  The call to getContent() is empty, even though data had been written to
 * the response's outputstream and is viewable by the http client which made the request.
 * <p/>
 * This project creates an ear file which provides the 'filter-test' filter which can be included in the filter chain.
 * <p/>
 * If the call to getContent() is empty, this filter throws and exception and the response from the origin service
 * is received by the client.
 * <p/>
 * If the call to getContent() provides the response, this filter appends additional content to the response.
 * <p/>
 * PS - ServletResponse.getContentType() returns null as well, although the content type can be accessed through the
 * call to ServletResponse.getHeaders()
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

        //WE PREOPTIMIZED FOR PERFORMANCE OR SOMETHING
        MutableHttpServletResponse mutableResponse = MutableHttpServletResponse.wrap((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
        mutableResponse.commitBufferToServletOutputStream();

        filterChain.doFilter(mutableRequest, mutableResponse);

        HttpServletRequest req = (HttpServletRequest) servletRequest;

        // Print out info from request & response wrapper
        LOG.debug("mutable response committed?: " + mutableResponse.isCommitted());
        mutableResponse.commitBufferToServletOutputStream();
        LOG.debug("after committing: " + mutableResponse.isCommitted());
        LOG.debug("URI: " + req.getRequestURI());
        LOG.debug("Status: " + mutableResponse.getStatus());
        //UNRELIABLE
        LOG.debug("mutable content-type: " + mutableResponse.getContentType());
        //UNRELIABLE
        LOG.debug("regular content-type: " + servletRequest.getContentType());
        LOG.debug("resp Header 'Content-Type: " + mutableResponse.getHeader("Content-Type"));
        //UNRELIABLE -- Why does this not work? OH GOD
        LOG.debug("Has body: " + mutableResponse.hasBody());

        String content = "";
        Scanner s = new Scanner(mutableResponse.getInputStream()).useDelimiter("\\A");
        if (s.hasNext()) {
            content = s.next();
        }

        LOG.debug("Content Body: '" + content + "'");

        // verify that the content is not empty.  This fails in repose but works in tomcat
        if (content.isEmpty()) {
            throw new RuntimeException("Content is empty");
        }

        //This should add stuff that we read to the response. Why doesn't it work?
        servletResponse.getWriter().write(content + "<extra> Added by TestFilter, should also see the rest of the content </extra>");
        servletResponse.getWriter().flush();
    }

    @Override
    public void destroy() {
    }
}
