package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

public class ResponseHeaderContainer implements HeaderContainer {

  private final HttpServletResponse response;
  private final List<String> headerNames;
  private final Map<String, List<HeaderValue>> headerValues;

  public ResponseHeaderContainer(HttpServletResponse response) {
    this.response = response;
    this.headerNames = extractHeaderNames();
    this.headerValues = extractHeaderValues();
  }

  private List<String> extractHeaderNames() {
    List<String> result = new LinkedList<String>();
    if (response != null) {
      Collection<String> names = response.getHeaderNames();

      for (String name : names) {
        result.add(name.toLowerCase());
      }
    }

    return result;
  }

  private Map<String, List<HeaderValue>> extractHeaderValues() {
    Map<String, List<HeaderValue>> valueMap = new HashMap<String, List<HeaderValue>>();

    if (response != null) {
      for (String name : getHeaderNames()) {
        HeaderFieldParser parser = new HeaderFieldParser(response.getHeaders(name));
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
    return HeaderContainerType.RESPONSE;
  }
}
