package com.rackspace.papi.filter;

import javax.servlet.Filter;

public class FilterContext {

    private final ClassLoader filterClassLoader;
    private final Filter filter;

    public FilterContext(Filter filter, ClassLoader filterClassLoader) {
        this.filter = filter;
        this.filterClassLoader = filterClassLoader;
    }

    public Filter getFilter() {
        return filter;
    }

    public ClassLoader getFilterClassLoader() {
        return filterClassLoader;
    }
}