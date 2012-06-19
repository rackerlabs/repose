package com.rackspace.papi.service.proxy.httpcomponent;

import javax.servlet.http.HttpServletResponse;

public class HttpComponentResponseCodeProcessor {
  private final int code;
  public HttpComponentResponseCodeProcessor(int code) {
    this.code = code;
  }
  
  public boolean isRedirect() {
    return code >= HttpServletResponse.SC_MULTIPLE_CHOICES && code < HttpServletResponse.SC_NOT_MODIFIED;
  }
  
  public boolean isNotModified() {
    return code == HttpServletResponse.SC_NOT_MODIFIED;
  }
  
  public int getCode() {
    return code;
  }
  
}
