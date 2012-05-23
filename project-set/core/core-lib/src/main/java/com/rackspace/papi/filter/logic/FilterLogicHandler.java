package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * Each Power API filter has an associated Handler that encapsulates the business logic for the filter.
 * This class enforces common entry points into a Handler class with a method for logic to be performed
 * before calling chain.doFilter() and a method for logic to be performed after calling chain.doFilter(),
 * i.e. as flow of control goes down the stack and as it comes back up the stack.
 *
 * @author jhopper
 */
public interface FilterLogicHandler {

    FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response);

    FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response);
}
