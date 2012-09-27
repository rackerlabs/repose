package org.openrepose.rackspace.auth_2_0.identity.parsers.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

public class NamespaceFilter extends XMLFilterImpl {

   private String usedNamespaceUri;
   private boolean addNamespace;

   //State variable
   private boolean addedNamespace = false;

   public NamespaceFilter(String namespaceUri, boolean addNamespace) {
      super();

      if (addNamespace) {
         this.usedNamespaceUri = namespaceUri;
      }
      else {
         this.usedNamespaceUri = "";
      }

      this.addNamespace = addNamespace;
   }

   @Override
   public void startElement(String arg0, String arg1, String arg2, Attributes arg3) throws SAXException {
      if (this.addNamespace && !this.addedNamespace) {

         super.startElement(this.usedNamespaceUri, arg1, arg2, arg3);

         // Make sure we don't do it twice
         this.addedNamespace = true;
      } else {
         super.startElement(arg0, arg1, arg2, arg3);   
      }
   }
}
