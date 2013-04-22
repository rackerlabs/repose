package com.rackspace.papi.httpx.processor.json.elements;

import com.rackspace.papi.httpx.processor.common.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class EndElement extends BaseElement implements Element {

    //Do not remove the name string from the list of parameters.
   public EndElement(String element, String name) {
      super(element);
   }

   @Override
   public void outputElement(ContentHandler handler) throws SAXException {
      handler.endElement(JSONX_URI, getLocalName(), getQname());
   }
}
