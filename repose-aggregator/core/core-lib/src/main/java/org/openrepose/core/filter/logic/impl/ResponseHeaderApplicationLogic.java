package org.openrepose.core.filter.logic.impl;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.filter.logic.HeaderApplicationLogic;

import java.util.Set;

public class ResponseHeaderApplicationLogic implements HeaderApplicationLogic {

  private final MutableHttpServletResponse response;

  public ResponseHeaderApplicationLogic(final MutableHttpServletResponse response) {
    this.response = response;
  }

  @Override
  public void removeHeader(String headerName) {
    response.removeHeader(headerName);
  }

  @Override
  public void addHeader(String key, Set<String> values) {
    for (String value : values) {
      response.addHeader(key, value);
    }
  }

  @Override
  public void removeAllHeaders() {
    response.removeAllHeaders();
  }
}
