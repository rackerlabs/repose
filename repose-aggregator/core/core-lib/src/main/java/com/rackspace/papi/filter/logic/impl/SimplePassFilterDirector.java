package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.logic.AbstractFilterDirector;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class SimplePassFilterDirector extends AbstractFilterDirector {

  private static final FilterDirector SINGLETON_INSTANCE = new SimplePassFilterDirector();
  private static final String EMPTY_STRING = "";

  public static FilterDirector getInstance() {
    return SINGLETON_INSTANCE;
  }
  private final HeaderManager emptyHeaderManager;

  private SimplePassFilterDirector() {
    emptyHeaderManager = new EmptyHeaderManager();
  }

  @Override
  public FilterAction getFilterAction() {
    return FilterAction.PASS;
  }

  @Override
  public HttpStatusCode getResponseStatus() {
    return HttpStatusCode.OK;
  }

  @Override
  public String getResponseMessageBody() {
    return EMPTY_STRING;
  }

  @Override
  public HeaderManager requestHeaderManager() {
    return emptyHeaderManager;
  }

  @Override
  public HeaderManager responseHeaderManager() {
    return emptyHeaderManager;
  }
}

class EmptyHeaderManager implements HeaderManager {

  @Override
  public void putHeader(String key, String... values) {
  }

  @Override
  public Map<String, Set<String>> headersToAdd() {
    return Collections.EMPTY_MAP;
  }

  @Override
  public Set<String> headersToRemove() {
    return Collections.EMPTY_SET;
  }

  @Override
  public void removeHeader(String key) {
  }

  @Override
  public boolean hasHeaders() {
    return false;
  }

  @Override
  public void applyTo(MutableHttpServletRequest request) {
  }

  @Override
  public void applyTo(MutableHttpServletResponse response) {
  }

  @Override
  public void appendHeader(String key, String... values) {
  }

  @Override
  public void appendHeader(String key, String value, Double quality) {
  }

  @Override
  public void appendToHeader(HttpServletRequest request, String key, String value) {
  }

  @Override
  public void removeAllHeaders() {
  }

  @Override
  public void appendDateHeader(String key, long value) {
  }
}
