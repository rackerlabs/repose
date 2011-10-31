package com.rackspace.papi.components.translation.preprocessor;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public interface Element {
   public void outputElement(ContentHandler handler) throws SAXException;
}
