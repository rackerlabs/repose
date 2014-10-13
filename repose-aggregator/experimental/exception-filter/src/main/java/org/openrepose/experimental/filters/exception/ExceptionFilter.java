package org.openrepose.experimental.filters.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

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
public class ExceptionFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionFilter.class);


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.warn("start " + this.getClass());    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        LOG.warn("in the doFilter method of ExceptionFilter.  About to throw an error!");

        //currently, we just want to validate that this filter throws an exception that's caught by repose core (powerfilterchain)
        throw new RuntimeException("This is just a test filter!  Don't use it in real life!");
    }

    @Override
    public void destroy() {
    }
}
