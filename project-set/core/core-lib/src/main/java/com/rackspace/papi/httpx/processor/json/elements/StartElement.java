package com.rackspace.papi.httpx.processor.json.elements;

import com.rackspace.papi.httpx.processor.common.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class StartElement extends BaseElement implements Element {
      
      public StartElement(String element, String name) {
         super(element);
         if (name != null) {
            getAttributes().addAttribute("", "name", "name", "xsd:string", name);
         }
      }
      
      @Override
      public void outputElement(ContentHandler handler) throws SAXException {
         handler.startElement(JSONX_URI, getLocalName(), getQname(), getAttributes());
         
      }
   
}
