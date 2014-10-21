package org.openrepose.core.filter;

import org.openrepose.commons.utils.Destroyable;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.regex.Pattern;

import javax.servlet.Filter;

/**
 * Holds information about a filter, the filter itself and the filter's application context.
 */
public class FilterContext implements Destroyable {

    private final Filter filter;
    private final org.openrepose.core.systemmodel.Filter filterConfig;
    private final String name;
    private final String uriRegex;
    private final Pattern uriPattern;
    private final AbstractApplicationContext filterAppContext;

    public FilterContext(Filter filter, AbstractApplicationContext filterAppContext) {
        this(filter, filterAppContext, null);
    }

    public FilterContext(Filter filter, AbstractApplicationContext filterAppContext, org.openrepose.core.systemmodel.Filter filterConfig) {
        this.filter = filter;
        this.filterAppContext = filterAppContext;
        this.filterConfig = filterConfig;
        if (filterConfig != null && filterConfig.getUriRegex() != null) {
            filterConfig.getName();
            this.name = filterConfig.getName();
            this.uriRegex = filterConfig.getUriRegex();
            this.uriPattern = Pattern.compile(uriRegex);
        } else {
            this.name = "n/a";
            this.uriRegex = ".*";
            this.uriPattern = Pattern.compile(this.uriRegex);
        }

    }

    public Filter getFilter() {
        return filter;
    }

    public org.openrepose.core.systemmodel.Filter getFilterConfig() {
        return filterConfig;
    }

    public Pattern getUriPattern() {
        return uriPattern;
    }

    public String getName() {
        return name;
    }

    public boolean isFilterAvailable() {
        return filter != null;
    }

    public String getUriRegex() {
        return uriRegex;
    }

    public AbstractApplicationContext getFilterAppContext() {
        return filterAppContext;
    }

    @Override
    public void destroy() {
        if (filter != null) {
            filter.destroy();
        }
        if(filterAppContext != null) {
            filterAppContext.close();
        }
    }
}