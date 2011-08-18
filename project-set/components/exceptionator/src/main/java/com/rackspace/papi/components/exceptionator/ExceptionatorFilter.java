package com.rackspace.papi.components.exceptionator;

import com.rackspace.papi.commons.util.StringUtilities;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 7/27/11
 * Time: 3:28 PM
 */
public class ExceptionatorFilter implements Filter {
    public static final String EXCEPTION_MESSAGE_HEADER = "X-Runtime-Exception-Message";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final String exceptionMessage = ((HttpServletRequest) request).getHeader(EXCEPTION_MESSAGE_HEADER);

        if (StringUtilities.isNotBlank(exceptionMessage)) {
            throw new IrishMythicalHeroException(exceptionMessage);
        }
    }

    @Override
    public void destroy() {
    }
}
