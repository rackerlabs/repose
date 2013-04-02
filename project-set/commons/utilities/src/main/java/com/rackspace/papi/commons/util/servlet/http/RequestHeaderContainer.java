package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class RequestHeaderContainer implements HeaderContainer {

  private final HttpServletRequest request;
  private final List<String> headerNames;
  private final Map<String, List<HeaderValue>> headerValues;

  public RequestHeaderContainer(HttpServletRequest request) {
    this.request = request;
    this.headerNames = extractHeaderNames();
    this.headerValues = extractHeaderValues();
  }

  private List<String> extractHeaderNames() {
    List<String> result = new LinkedList<String>();
    if (request != null) {
      Enumeration<String> names = request.getHeaderNames();

      if (names != null) {
        while (names.hasMoreElements()) {
          result.add(names.nextElement().toLowerCase());
        }
      }
    }

    return result;
  }

  private Map<String, List<HeaderValue>> extractHeaderValues() {
    Map<String, List<HeaderValue>> valueMap = new HashMap<String, List<HeaderValue>>();

    if (request != null) {
      for (String name : getHeaderNames()) {
        HeaderFieldParser parser = new HeaderFieldParser(request.getHeaders(name));
        valueMap.put(name, parser.parse());
      }
    }

    return valueMap;
  }

  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
  @Override
  public List<String> getHeaderNames() {
    return headerNames;
  }

  @Override
  public List<HeaderValue> getHeaderValues(String name) {
    return headerValues.get(name);
  }

  @Override
  public boolean containsHeader(String name) {
    List<HeaderValue> values = getHeaderValues(name);
    return values != null && !values.isEmpty();
  }

  @Override
  public HeaderContainerType getContainerType() {
    return HeaderContainerType.REQUEST;
  }
}
