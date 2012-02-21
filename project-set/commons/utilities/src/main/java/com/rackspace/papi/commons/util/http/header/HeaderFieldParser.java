package com.rackspace.papi.commons.util.http.header;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author zinic
 */
public class HeaderFieldParser {

   private final List<String> headerValueStrings;

   private HeaderFieldParser() {
      headerValueStrings = new LinkedList<String>();
   }

   public HeaderFieldParser(String rawHeaderString) {
      this();
      
      addValue(rawHeaderString);
   }

   public HeaderFieldParser(Enumeration<String> headerValueEnumeration) {
      this();
      
      while(headerValueEnumeration.hasMoreElements()) {
         addValue(headerValueEnumeration.nextElement());
      }
   }
   
   public HeaderFieldParser(Set<String> headers) {
      this();
      
      for(String header: headers) {
         addValue(header);
      }
   }
   
   private void addValue(String rawHeaderString) {
      final String[] splitHeaderValues = rawHeaderString.split(",");
      
      for (String splitHeaderValue : splitHeaderValues) {
         headerValueStrings.add(splitHeaderValue.trim());
      }
   }
   
   public List<HeaderValue> parse() {
      final List<HeaderValue> headerValues = new LinkedList<HeaderValue>();
      
      for (String headerValueString : headerValueStrings) {
         headerValues.add(new HeaderValueParser(headerValueString).parse());
      }
      
      return headerValues;
   }
}
