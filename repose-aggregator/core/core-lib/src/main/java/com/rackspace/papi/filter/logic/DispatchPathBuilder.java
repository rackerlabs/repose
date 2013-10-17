package com.rackspace.papi.filter.logic;


public class DispatchPathBuilder {
  private final String requestPath;
  private final String context;
  
  public DispatchPathBuilder(String request, String context) {
    this.requestPath = request;
    this.context = context;
  }
  
  public String build() {
    String dispatchPath = requestPath;
    if (dispatchPath.startsWith(context)) {
      dispatchPath = dispatchPath.substring(context.length());
    }

    return dispatchPath;
  }
}
