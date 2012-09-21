package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.xslt.AbstractChainExecutor;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;

public class XsltFilterChainExecutor extends AbstractChainExecutor {

   private final XsltFilterChain chain;
   private final Properties format = new Properties();

   public XsltFilterChainExecutor(XsltFilterChain chain) {
      this.chain = chain;
      format.put(OutputKeys.METHOD, "xml");
      format.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
      format.put(OutputKeys.ENCODING, "UTF-8");
      format.put(OutputKeys.INDENT, "yes");
   }

   public void executeChain(InputStream in, OutputStream output, List<XsltParameter> inputs, List<XsltParameter<? extends OutputStream>> outputs) throws XsltException {
      try {
         for (TransformReference filter : chain.getFilters()) {
            // pass the input stream to all transforms as a param inputstream
            net.sf.saxon.Filter saxonFilter = (net.sf.saxon.Filter) filter.getFilter();
            Transformer transformer = saxonFilter.getTransformer();
            setInputParameters(filter.getId(), transformer, inputs);
            setAlternateOutputs(filter.getId(), transformer, outputs);
         }

         Transformer transformer = chain.getFactory().newTransformer();
         transformer.setOutputProperties(format);
         transformer.transform(getSAXSource(new InputSource(in)), new StreamResult(output));
      } catch (TransformerException ex) {
         throw new XsltException(ex);
      }
   }

   protected SAXSource getSAXSource(InputSource source) {
      if (chain.getFilters().isEmpty()) {
         return new SAXSource(source);
      }

      XMLFilter lastFilter = chain.getFilters().get(chain.getFilters().size() - 1).getFilter();
      
      return new SAXSource(lastFilter, source);
   }
}
