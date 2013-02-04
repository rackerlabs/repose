package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.QualityFactorHeaderChooser;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

public final class RequestHeaderValuesImpl implements RequestHeaderValues {

  private static final String REQUEST_HEADERS_ATTRIBUTE = "repose.request.headers";
  private final Map<String, List<String>> headers;

  public static RequestHeaderValues extract(HttpServletRequest request) {
    return new RequestHeaderValuesImpl(request);
  }

  private RequestHeaderValuesImpl(HttpServletRequest request) {
    this.headers = initHeaders(request);
    cloneHeaders(request);
  }

  private Map<String, List<String>> initHeaders(HttpServletRequest request) {
    Map<String, List<String>> currentHeaderMap = (Map<String, List<String>>) request.getAttribute(REQUEST_HEADERS_ATTRIBUTE);

    if (currentHeaderMap == null) {
      currentHeaderMap = new HashMap<String, List<String>>();
      request.setAttribute(REQUEST_HEADERS_ATTRIBUTE, currentHeaderMap);
    }

    return currentHeaderMap;
  }

  private void cloneHeaders(HttpServletRequest request) {

    final Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    final Enumeration<String> headerNames = request.getHeaderNames();

    while (headerNames != null && headerNames.hasMoreElements()) {
      //Normalize to lowercase
      final String headerName = headerNames.nextElement().toLowerCase();

      final Enumeration<String> headerValues = request.getHeaders(headerName);
      final List<String> copiedHeaderValues = new LinkedList<String>();

      while (headerValues.hasMoreElements()) {
        copiedHeaderValues.add(headerValues.nextElement());
      }

      headerMap.put(headerName, copiedHeaderValues);
    }

    headers.clear();
    headers.putAll(headerMap);
  }

  @Override
  public void addHeader(String name, String value) {
    final String lowerCaseName = name.toLowerCase();

    List<String> headerValues = headers.get(lowerCaseName);

    if (headerValues == null) {
      headerValues = new LinkedList<String>();
    }

    headerValues.add(value);

    headers.put(lowerCaseName, headerValues);
  }

  @Override
  public void replaceHeader(String name, String value) {
    final List<String> headerValues = new LinkedList<String>();

    headerValues.add(value);

    headers.put(name.toLowerCase(), headerValues);
  }

  @Override
  public void removeHeader(String name) {
    headers.remove(name.toLowerCase());
  }

  @Override
  public void clearHeaders() {
    headers.clear();
  }

  @Override
  public String getHeader(String name) {
    return fromMap(headers, name.toLowerCase());
  }

  static String fromMap(Map<String, List<String>> headers, String headerName) {
    final List<String> headerValues = headers.get(headerName);

    return (headerValues != null && headerValues.size() > 0) ? headerValues.get(0) : null;
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return Collections.enumeration(headers.keySet());
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    final List<String> headerValues = headers.get(name.toLowerCase());

    return Collections.enumeration(headerValues != null ? headerValues : Collections.EMPTY_SET);
  }

  @Override
  public List<HeaderValue> getPreferredHeaderValues(String name, HeaderValue defaultValue) {
    HeaderFieldParser parser = new HeaderFieldParser(headers.get(name.toLowerCase()));
    List<HeaderValue> headerValues = parser.parse();

    QualityFactorHeaderChooser chooser = new QualityFactorHeaderChooser<HeaderValue>();
    List<HeaderValue> values = chooser.choosePreferredHeaderValues(headerValues);

    if (values.isEmpty() && defaultValue != null) {
      values.add(defaultValue);
    }

    return values;

  }

  @Override
  public List<HeaderValue> getPreferredHeaders(String name, HeaderValue defaultValue) {

    HeaderFieldParser parser = new HeaderFieldParser(headers.get(name.toLowerCase()));
    List<HeaderValue> headerValues = parser.parse();

    Map<Double, List<HeaderValue>> groupedHeaderValues = new LinkedHashMap<Double, List<HeaderValue>>();

    for (HeaderValue value : headerValues) {

      if (!groupedHeaderValues.keySet().contains(value.getQualityFactor())) {
        groupedHeaderValues.put(value.getQualityFactor(), new LinkedList<HeaderValue>());
      }

      groupedHeaderValues.get(value.getQualityFactor()).add(value);
    }

    headerValues.clear();

    List<Double> qualities = new ArrayList<Double>(groupedHeaderValues.keySet());
    java.util.Collections.sort(qualities);
    java.util.Collections.reverse(qualities);

    for (Double quality : qualities) {
      headerValues.addAll(groupedHeaderValues.get(quality));
    }

    if (headerValues.isEmpty() && defaultValue != null) {
      headerValues.add(defaultValue);
    }

    return headerValues;
  }
}
