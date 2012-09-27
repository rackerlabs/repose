package com.rackspace.papi.http.proxy.common;

import javax.servlet.http.HttpServletResponse;

public class HttpResponseCodeProcessor {
  private final int code;
  public HttpResponseCodeProcessor(int code) {
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
