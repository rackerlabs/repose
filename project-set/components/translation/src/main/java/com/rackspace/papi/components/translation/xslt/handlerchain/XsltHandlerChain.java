package com.rackspace.papi.components.translation.xslt.handlerchain;

import com.rackspace.papi.components.translation.xslt.AbstractChain;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltException;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Templates;
import javax.xml.transform.sax.SAXTransformerFactory;

public class XsltHandlerChain extends AbstractChain<Templates> {
   
   public XsltHandlerChain(SAXTransformerFactory factory) {
      super(factory, new ArrayList<TransformReference<Templates>>());
   }
   
   public XsltHandlerChain(SAXTransformerFactory factory, List<TransformReference<Templates>> handlers) {
       super(factory, handlers);
   }
   
    @Override
   public synchronized void executeChain(InputStream in, OutputStream output, List<XsltParameter> inputs, List<XsltParameter<? extends OutputStream>> outputs) throws XsltException  {
      new XsltHandlerChainExecutor(getFactory(), this).executeChain(in, output, inputs, outputs);
   }
}
