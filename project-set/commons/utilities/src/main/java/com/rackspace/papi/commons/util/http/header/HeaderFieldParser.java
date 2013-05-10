package com.rackspace.papi.commons.util.http.header;

import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author zinic
 */
public class HeaderFieldParser {

  private Pattern date = Pattern.compile("[^\\d]{3},\\s*[\\d]{2}\\s*[^\\d]{3}\\s*[\\d]{4}\\s*[\\d]{2}:[\\d]{2}:[\\d]{2}\\s*GMT");


   
  private final List<String> headerValueStrings;

  private HeaderFieldParser() {
    headerValueStrings = new LinkedList<String>();
  }

  public HeaderFieldParser(String rawHeaderString) {
    this();

    if (rawHeaderString != null) {
      addValue(rawHeaderString);
    }
  }

  public HeaderFieldParser(Enumeration<String> headerValueEnumeration) {
    this();

    if (headerValueEnumeration != null) {
      while (headerValueEnumeration.hasMoreElements()) {
        addValue(headerValueEnumeration.nextElement());
      }
    }
  }
  
  
  public HeaderFieldParser(Collection<String> headers) {
    this();

    if (headers != null) {
      for (String header : headers) {
        addValue(header);
      }
    }
  }

  private void addValue(String rawHeaderString) {
    Matcher matcher = date.matcher(rawHeaderString);
    if (matcher.matches()) {
      // This is an RFC 1123 date string
      headerValueStrings.add(rawHeaderString);
      return;
    }
    
    final String[] splitHeaderValues = rawHeaderString.split(",");

    for (String splitHeaderValue : splitHeaderValues) {
      if (!splitHeaderValue.isEmpty()) {
        headerValueStrings.add(splitHeaderValue.trim());
      }
    }
  }

  public List<HeaderValue> parse() {
    final List<HeaderValue> headerValues = new LinkedList<HeaderValue>();

    for (String headerValueString : headerValueStrings) {
      headerValues.add(new HeaderValueParser(headerValueString).parse());
    }

    return headerValues;
  }
  
  public Pattern getDate() {
        return date;
    }

}
