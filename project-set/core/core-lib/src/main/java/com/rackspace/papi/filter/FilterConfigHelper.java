package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.StringUtilities;
import javax.servlet.FilterConfig;

public class FilterConfigHelper {
    public static final String FILTER_CONFIG = "filter-config";
    private final FilterConfig filterConfig;
    
    public FilterConfigHelper(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }
    
    public String getFilterConfig(String defaultConfig) {
        return StringUtilities.getNonBlankValue(filterConfig.getInitParameter(FILTER_CONFIG), defaultConfig);
    }
}
