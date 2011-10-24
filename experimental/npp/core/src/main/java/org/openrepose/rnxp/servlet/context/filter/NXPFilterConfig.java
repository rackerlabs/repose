package org.openrepose.rnxp.servlet.context.filter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

/**
 *
 * @author zinic
 */
public class NXPFilterConfig implements FilterConfig {

    private final ServletContext servletContext;
    private final Map<String, String> filterInitParameters;
    private final String filterName;

    public NXPFilterConfig(String filterName, ServletContext servletContext, Map<String, String> filterInitParameters) {
        this.servletContext = servletContext;
        this.filterInitParameters = filterInitParameters;
        this.filterName = filterName;
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public String getInitParameter(String name) {
        return filterInitParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(filterInitParameters.keySet());
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }
}
