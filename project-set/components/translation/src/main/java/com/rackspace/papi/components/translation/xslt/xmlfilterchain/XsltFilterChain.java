package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.xslt.Parameter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.xml.sax.XMLFilter;

public class XsltFilterChain {
   private final SAXTransformerFactory factory;
   private final List<XmlFilterReference> filters ;
   
   public XsltFilterChain(SAXTransformerFactory factory) {
      this(factory, new ArrayList<XmlFilterReference>());
   }
   
   public XsltFilterChain(SAXTransformerFactory factory, List<XmlFilterReference> filters) {
      this.factory = factory;
      this.filters = filters;
   }
   
   public SAXTransformerFactory getFactory() {
      return factory;
   }
   
   public List<XmlFilterReference> getFilters() {
      return filters;
   }
   
   public void executeChain(InputStream in, OutputStream output, List<Parameter> inputs, List<Parameter<? extends OutputStream>> outputs) throws XsltFilterException {
      new XsltFilterChainExecutor(this).executeChain(in, output, inputs, outputs);
   }
   
}
