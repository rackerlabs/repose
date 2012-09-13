package com.rackspace.papi.components.translation.xslt.handlerchain;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

public class XsltHandlerChain {
   private final SAXTransformerFactory factory;
   private final List<TransformerHandler> handlers ;
   
   public XsltHandlerChain(SAXTransformerFactory factory) {
      this(factory, new ArrayList<TransformerHandler>());
   }
   
   public XsltHandlerChain(SAXTransformerFactory factory, List<TransformerHandler> handlers) {
      this.factory = factory;
      this.handlers = handlers;
   }
   
   public SAXTransformerFactory getFactory() {
      return factory;
   }
   
   public List<TransformerHandler> getHandlers() {
      return handlers;
   }
   
   public synchronized void executeChain(InputStream in, OutputStream output, List<Parameter> inputs, List<Parameter<? extends OutputStream>> outputs) throws XsltHandlerException  {
      new XsltHandlerChainExecutor(this).executeChain(in, output, inputs, outputs);
   }
   
}
