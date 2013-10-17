package com.rackspace.papi.httpx.processor.json.elements;

import com.rackspace.papi.httpx.processor.common.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ScalarElement<T> extends BaseElement implements Element {

   private final T value;

   public ScalarElement(String element, String name, T value) {
      super(element);
      
      this.value = value;
      if (name != null) {
         getAttributes().addAttribute("", "name", "name", "xsd:string", name);
      }
   }

   @Override
   public void outputElement(ContentHandler handler) throws SAXException {
      handler.startElement(JSONX_URI, getLocalName(), getQname(), getAttributes());
      if (value != null) {
         char[] charValue = value.toString().toCharArray();
         handler.characters(charValue, 0, charValue.length);
      }
      handler.endElement(JSONX_URI, getLocalName(), getQname());
   }
}
