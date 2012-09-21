package com.rackspace.papi.components.translation.xslt.xmlfilterchain2;

import com.rackspace.papi.components.translation.xslt.AbstractXsltChain;
import com.rackspace.papi.components.translation.xslt.Parameter;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Templates;
import javax.xml.transform.sax.SAXTransformerFactory;

public class XsltFilterChain extends AbstractXsltChain<Templates> {
   
   public XsltFilterChain(SAXTransformerFactory factory) {
      super(factory, new ArrayList<TransformReference<Templates>>());
   }
   
   public XsltFilterChain(SAXTransformerFactory factory, List<TransformReference<Templates>> filters) {
       super(factory, filters);
   }
   
    @Override
   public void executeChain(InputStream in, OutputStream output, List<Parameter> inputs, List<Parameter<? extends OutputStream>> outputs) throws XsltException {
        if (in == null || output == null) {
            return;
        }
      new XsltFilterChainExecutor(getFactory(), this).executeChain(in, output, inputs, outputs);
   }
   
}
