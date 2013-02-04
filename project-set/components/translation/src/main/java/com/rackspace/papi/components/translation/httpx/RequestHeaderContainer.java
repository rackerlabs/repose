package com.rackspace.papi.components.translation.httpx;

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

      while (names.hasMoreElements()) {
        result.add(names.nextElement());
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

  @Override
  public List<String> getHeaderNames() {
    return headerNames;
  }

  @Override
  public List<HeaderValue> getHeaderValues(String name) {
    return headerValues.get(name);
  }
}
