package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.filter.logic.HeaderApplicationLogic;

import java.util.Set;

public class RequestHeaderApplicationLogic implements HeaderApplicationLogic {

  private final MutableHttpServletRequest request;

  public RequestHeaderApplicationLogic(final MutableHttpServletRequest request) {
    this.request = request;
  }

  @Override
  public void removeHeader(String headerName) {
    request.removeHeader(headerName);
  }

  @Override
  public void addHeader(String key, Set<String> values) {
    for (String value : values) {
      request.addHeader(key, value);
    }
  }

  @Override
  public void removeAllHeaders() {
    request.clearHeaders();
  }
}
