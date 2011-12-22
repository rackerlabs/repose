package com.rackspace.papi.httpx.processor.json.elements;

import org.xml.sax.helpers.AttributesImpl;

public class BaseElement {

   public static final String QNAME_PREFIX = "json";
   public static final String JSONX_URI = "http://www.ibm.com/xmlns/prod/2009/jsonx";
   private final AttributesImpl attrs;
   private final String element;

   public BaseElement(String element) {
      this.element = element;
      this.attrs = new AttributesImpl();
   }
   
   public BaseElement(String element, AttributesImpl attrs) {
      this.element = element;
      this.attrs = attrs;
   }
   
   public String getElement() {
      return element;
   }
   
   public String getLocalName() {
      return getLocalName(element);
   }
   
   public String getQname() {
      return getQname(element);
   }
   
   public AttributesImpl getAttributes() {
      return attrs;
   }
   
   protected static String getLocalName(String name) {
      String[] parts = name.split(":");
      return parts[parts.length - 1];
   }

   protected static String getQname(String name) {
      return QNAME_PREFIX + ":" + getLocalName(name);
   }
}
