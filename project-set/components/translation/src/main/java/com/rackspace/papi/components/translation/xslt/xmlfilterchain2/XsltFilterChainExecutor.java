package com.rackspace.papi.components.translation.xslt.xmlfilterchain2;

import com.rackspace.papi.components.translation.xslt.AbstractChainExecutor;
import com.rackspace.papi.components.translation.xslt.Parameter;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;

public class XsltFilterChainExecutor extends AbstractChainExecutor {

    private final XsltFilterChain chain;
    private final Properties format = new Properties();

    public XsltFilterChainExecutor(SAXTransformerFactory factory, XsltFilterChain chain) {
        super(factory);
        this.chain = chain;
        format.put(OutputKeys.METHOD, "xml");
        format.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        format.put(OutputKeys.ENCODING, "UTF-8");
        format.put(OutputKeys.INDENT, "yes");
    }

    public void executeChain(InputStream in, OutputStream output, List<Parameter> inputs, List<Parameter<? extends OutputStream>> outputs) throws XsltException {
        try {
            for (TransformReference<Templates> filter : chain.getFilters()) {
                // pass the input stream to all transforms as a param inputstream
                net.sf.saxon.Filter saxonFilter = (net.sf.saxon.Filter) getFactory().newXMLFilter(filter.getFilter());
                Transformer transformer = saxonFilter.getTransformer();
                setInputParameters(filter.getId(), transformer, inputs);
                setAlternateOutputs(filter.getId(), transformer, outputs);
            }

            Transformer transformer = getFactory().newTransformer();
            transformer.setOutputProperties(format);
            transformer.transform(getSAXSource(new InputSource(in)), new StreamResult(output));
        } catch (TransformerException ex) {
            throw new XsltException(ex);
        }
    }

    protected SAXSource getSAXSource(InputSource source) throws TransformerConfigurationException {
        if (chain.getFilters().isEmpty()) {
            return new SAXSource(source);
        }
        
        Templates lastTemplate = chain.getFilters().get(chain.getFilters().size() - 1).getFilter();

        return new SAXSource(getFactory().newXMLFilter(lastTemplate), source);
    }
}
