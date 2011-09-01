package com.rackspace.papi.filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author fran
 *
 * Cases to handle/test:
 * 1.  There are no filters in our chain but some in container's
 * 2.  There are filters in our chain and in container's
 * 3.  There are no filters in our chain or container's
 * 4.  There are filters in our chain but none in container's
 * 5.  If one of our filters breaks out of the chain (i.e. it doesn't call doFilter), then we shouldn't call
 *     doFilter on the container's filter chain.
 * 6.  If one of the container's filters breaks out of the chain then our chain should unwind correctly
 * 
 */
public class RequestFilterChainState implements javax.servlet.FilterChain {

    private final List<FilterContext> filterChainCopy;
    private final FilterChain containerFilterChain;
    private final ClassLoader containerClassLoader;
    private int position;

    public RequestFilterChainState(List<FilterContext> filterChainCopy, FilterChain containerFilterChain) {
        this.filterChainCopy = new LinkedList<FilterContext>(filterChainCopy);
        this.containerFilterChain = containerFilterChain;
        this.containerClassLoader = Thread.currentThread().getContextClassLoader();
    }
    
    public void startFilterChain(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        doFilter(servletRequest, servletResponse);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        if (position < filterChainCopy.size()) {
            final FilterContext nextFilterContext = filterChainCopy.get(position++);

            final Thread currentThread = Thread.currentThread();
            final ClassLoader previousClassLoader = currentThread.getContextClassLoader();
            final ClassLoader nextClassLoader = nextFilterContext.getFilterClassLoader();

            currentThread.setContextClassLoader(nextClassLoader);

            try {
                nextFilterContext.getFilter().doFilter(servletRequest, servletResponse, this);
            } finally {
                currentThread.setContextClassLoader(previousClassLoader);
            }
        } else {
            final Thread currentThread = Thread.currentThread();
            final ClassLoader previousClassLoader = currentThread.getContextClassLoader();

            currentThread.setContextClassLoader(containerClassLoader);

            try {
                containerFilterChain.doFilter(servletRequest, servletResponse);
            } finally {
                currentThread.setContextClassLoader(previousClassLoader);
            }
        }
    }
}
