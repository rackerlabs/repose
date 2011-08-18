package com.rackspace.papi.filter;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author fran
 */
public class PowerFilterChain implements javax.servlet.FilterChain {
    private final List<FilterContext> filterChain;
    private int position;

    public PowerFilterChain() {
        this.filterChain = new LinkedList<FilterContext>();
    }

    public PowerFilterChain(List<FilterContext> filterChain) {
        this.filterChain = new LinkedList<FilterContext>(filterChain);
    }
    
    @Override
    public synchronized void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        if (position < filterChain.size()) {
            final FilterContext nextFilterContext = filterChain.get(position++);

            final Thread currentThread = Thread.currentThread();
            final ClassLoader previousClassLoader = currentThread.getContextClassLoader();
            final ClassLoader nextClassLoader = nextFilterContext.getFilterClassLoader();

            currentThread.setContextClassLoader(nextClassLoader);

            try {
                nextFilterContext.getFilter().doFilter(servletRequest, servletResponse, this);
            } finally {
                currentThread.setContextClassLoader(previousClassLoader);
                position = 0;
            }
        }
    }

    public synchronized void update(PowerFilterChain newFilterChain) {
        if (newFilterChain.filterChain.size() > 0) {
            for (FilterContext ctx : filterChain) {
                ctx.getFilter().destroy();
            }

            filterChain.clear();

            filterChain.addAll(newFilterChain.filterChain);
        }
    }    

    public synchronized void destroy() {
        for (FilterContext ctx : filterChain) {
            ctx.getFilter().destroy();
        }

        filterChain.clear();
    }

    public synchronized Integer getSize() {
        return this.filterChain.size();
    }
}
