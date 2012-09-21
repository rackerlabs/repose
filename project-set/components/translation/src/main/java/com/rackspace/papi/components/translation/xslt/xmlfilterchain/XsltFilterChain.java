package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.xslt.AbstractChain;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.xml.sax.XMLFilter;

public class XsltFilterChain extends AbstractChain<XMLFilter> {
   
   public XsltFilterChain(SAXTransformerFactory factory) {
      super(factory, new ArrayList<TransformReference<XMLFilter>>());
   }
   
   public XsltFilterChain(SAXTransformerFactory factory, List<TransformReference<XMLFilter>> filters) {
       super(factory, filters);
   }
   
    @Override
   public void executeChain(InputStream in, OutputStream output, List<XsltParameter> inputs, List<XsltParameter<? extends OutputStream>> outputs) throws XsltException {
        if (in == null || output == null) {
            return;
        }
      new XsltFilterChainExecutor(this).executeChain(in, output, inputs, outputs);
   }
   
}
