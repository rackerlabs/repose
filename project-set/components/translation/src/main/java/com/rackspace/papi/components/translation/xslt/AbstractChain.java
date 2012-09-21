package com.rackspace.papi.components.translation.xslt;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import javax.xml.transform.sax.SAXTransformerFactory;

public abstract class AbstractChain<T> implements XsltChain<T> {
    private final SAXTransformerFactory factory;
    private final List<TransformReference<T>> filters;
    
    public AbstractChain(SAXTransformerFactory factory, List<TransformReference<T>> filters) {
      this.factory = factory;
      this.filters = filters;
    }

    @Override
    public abstract void executeChain(InputStream in, OutputStream output, List<XsltParameter> inputs, List<XsltParameter<? extends OutputStream>> outputs) throws XsltException;

    @Override
   public SAXTransformerFactory getFactory() {
      return factory;
   }
   
    @Override
   public List<TransformReference<T>> getFilters() {
      return filters;
   }
   
}
