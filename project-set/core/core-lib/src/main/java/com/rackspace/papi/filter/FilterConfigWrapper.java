package com.rackspace.papi.filter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;

/**
 *
 * @author Dan Daley
 */
public class FilterConfigWrapper implements FilterConfig {

  private final FilterConfig config;
  
  public FilterConfigWrapper(FilterConfig config) {
    this.config = config;
  }
  
  
  @Override
  public String getFilterName() {
    return config.getFilterName();
  }

  @Override
  public ServletContext getServletContext() {
    return new ServletContextWrapper(config.getServletContext());
  }

  @Override
  public String getInitParameter(String string) {
    return config.getInitParameter(string);
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    return config.getInitParameterNames();
  }
  
}
