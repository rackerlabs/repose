/*
 * 
 */
package com.rackspace.papi.filter.logic;


import com.rackspace.papi.commons.util.StringUriUtilities;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Dan Daley
 */
public class DispatchPathBuilder {
  private final ServletRequest request;
  private final String context;
  
  public DispatchPathBuilder(ServletRequest request, String context) {
    this.request = request;
    this.context = context;
  }
  
  public String build() {
    String dispatchPath = ((HttpServletRequest)request).getRequestURI();
    if (dispatchPath.startsWith(context)) {
      dispatchPath = dispatchPath.substring(context.length());
    }
    
    if(!dispatchPath.startsWith("/")){
        dispatchPath = StringUriUtilities.formatUri(dispatchPath);
    }
    
    return dispatchPath;
  }
}
