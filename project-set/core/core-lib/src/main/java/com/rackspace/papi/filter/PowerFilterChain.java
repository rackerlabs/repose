package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.filter.resource.ResourceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author fran
 *         <p/>
 *         Cases to handle/test: 1. There are no filters in our chain but some in container's 2. There are filters in our chain
 *         and in container's 3. There are no filters in our chain or container's 4. There are filters in our chain but none in
 *         container's 5. If one of our filters breaks out of the chain (i.e. it doesn't call doFilter), then we shouldn't call
 *         doFilter on the container's filter chain. 6. If one of the container's filters breaks out of the chain then our chain
 *         should unwind correctly
 */
public class PowerFilterChain implements FilterChain {

    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterChain.class);
    private static final String START_TIME_ATTRIBUTE = "com.rackspace.repose.logging.start.time";
    private final ResourceMonitor resourceMonitor;
    private final List<FilterContext> filterChainCopy;
    private final FilterChain containerFilterChain;
    private final ClassLoader containerClassLoader;
    private List<FilterContext> currentFilters;
    private int position;
    private final PowerFilterRouter router;
    private RequestTracer tracer = null;
    private boolean filterChainAvailable;

    public PowerFilterChain(List<FilterContext> filterChainCopy, FilterChain containerFilterChain,
            ResourceMonitor resourceMontior, PowerFilterRouter router, ReposeInstanceInfo instanceInfo)
            throws PowerFilterChainException {

        this.filterChainCopy = new LinkedList<FilterContext>(filterChainCopy);
        this.containerFilterChain = containerFilterChain;
        this.containerClassLoader = Thread.currentThread().getContextClassLoader();
        this.resourceMonitor = resourceMontior;
        this.router = router;
        Thread.currentThread().setName(instanceInfo.toString());
    }

    public void startFilterChain(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IOException, ServletException {
        resourceMonitor.use();

        try {
            final HttpServletRequest request = (HttpServletRequest) servletRequest;
            tracer = new RequestTracer(traceRequest(request));
            currentFilters = getFilterChainForRequest(request.getRequestURI());
            filterChainAvailable = isCurrentFilterChainAvailable();
            servletRequest.setAttribute("filterChainAvailableForRequest", filterChainAvailable);

            doFilter(servletRequest, servletResponse);
        } finally {
            resourceMonitor.released();
        }
    }

    /**
     * Find the filters that are applicable to this request based on the uri-regex specified for each filter and the
     * current request uri.
     * <p/>
     * If a necessary filter is not available, then return an empty filter list.
     *
     * @param uri
     * @return
     */
    private List<FilterContext> getFilterChainForRequest(String uri) {
        List<FilterContext> filters = new LinkedList<FilterContext>();
        for (FilterContext filter : filterChainCopy) {
            if ((filter.getUriPattern() == null || filter.getUriPattern().matcher(uri).matches())) {
                filters.add(filter);
            }
        }

        return filters;
    }

    private boolean traceRequest(HttpServletRequest request) {
        return request.getHeader("X-Trace-Request") != null;
    }

    private boolean isCurrentFilterChainAvailable() {
        boolean result = true;

        for (FilterContext filter : currentFilters) {
            if (!filter.isFilterAvailable()) {
                LOG.warn("Filter is not available for processing requests: " + filter.getName());
            }
            result &= filter.isFilterAvailable();
        }

        return result;
    }

    private boolean isResponseOk(HttpServletResponse response) {
        return response.getStatus() < HttpStatusCode.INTERNAL_SERVER_ERROR.intValue();
    }

    private ClassLoader setClassLoader(ClassLoader loader) {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader = currentThread.getContextClassLoader();

        currentThread.setContextClassLoader(loader);

        return previousClassLoader;
    }

    private void doReposeFilter(MutableHttpServletRequest mutableHttpRequest, ServletResponse servletResponse,
            FilterContext filterContext) throws IOException, ServletException {
        final MutableHttpServletResponse mutableHttpResponse =
                MutableHttpServletResponse.wrap(mutableHttpRequest, (HttpServletResponse) servletResponse);
        ClassLoader previousClassLoader = setClassLoader(filterContext.getFilterClassLoader());

        mutableHttpResponse.pushOutputStream();
        try {
            filterContext.getFilter().doFilter(mutableHttpRequest, mutableHttpResponse, this);
        } catch (Exception ex) {
            String filterName = filterContext.getFilter().getClass().getSimpleName();
            LOG.error("Failure in filter: " + filterName + "  -  Reason: " + ex.getMessage(), ex);
        } finally {
            mutableHttpResponse.popOutputStream();
            setClassLoader(previousClassLoader);
        }
    }

    private void doRouting(MutableHttpServletRequest mutableHttpRequest, ServletResponse servletResponse)
            throws IOException, ServletException {
        final MutableHttpServletResponse mutableHttpResponse =
                MutableHttpServletResponse.wrap(mutableHttpRequest, (HttpServletResponse) servletResponse);
        ClassLoader previousClassLoader = setClassLoader(containerClassLoader);

        try {
            if (isResponseOk(mutableHttpResponse)) {
                containerFilterChain.doFilter(mutableHttpRequest, mutableHttpResponse);
            }

            if (isResponseOk(mutableHttpResponse)) {
                router.route(mutableHttpRequest, mutableHttpResponse);
            }
        } catch (Exception ex) {
            LOG.error("Failure in filter within container filter chain. Reason: " + ex.getMessage(), ex);
            mutableHttpResponse.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());
            mutableHttpResponse.setLastException(ex);
        } finally {
            setClassLoader(previousClassLoader);
        }
    }

    private void setStartTimeForHttpLogger(long startTime, MutableHttpServletRequest mutableHttpRequest) {
        long start = startTime;

        if (startTime == 0) {
            start = System.currentTimeMillis();
        }
        mutableHttpRequest.setAttribute(START_TIME_ATTRIBUTE, start);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IOException, ServletException {
        final MutableHttpServletRequest mutableHttpRequest =
                MutableHttpServletRequest.wrap((HttpServletRequest) servletRequest);
        final MutableHttpServletResponse mutableHttpResponse =
                MutableHttpServletResponse.wrap(mutableHttpRequest, (HttpServletResponse) servletResponse);

        if (filterChainAvailable && position < currentFilters.size()) {
            FilterContext filter = currentFilters.get(position++);
            long start = tracer.traceEnter();
            setStartTimeForHttpLogger(start, mutableHttpRequest);
            doReposeFilter(mutableHttpRequest, servletResponse, filter);
            tracer.traceExit(mutableHttpResponse, filter.getFilterConfig().getName(), start);
        } else {
            long start = tracer.traceEnter();
            doRouting(mutableHttpRequest, servletResponse);
            tracer.traceExit(mutableHttpResponse, "route", start);
        }
    }
}
