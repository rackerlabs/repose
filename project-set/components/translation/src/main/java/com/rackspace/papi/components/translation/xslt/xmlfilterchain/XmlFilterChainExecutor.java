package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import com.rackspace.papi.components.translation.xslt.XsltException;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.xalan.transformer.TrAXFilter;
import org.slf4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class XmlFilterChainExecutor {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(XmlFilterChainExecutor.class);
    private final XmlFilterChain chain;
    private final Properties format = new Properties();

    public XmlFilterChainExecutor(XmlFilterChain chain) {
        this.chain = chain;
        format.put(OutputKeys.METHOD, "xml");
        format.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        format.put(OutputKeys.ENCODING, "UTF-8");
        format.put(OutputKeys.INDENT, "yes");
    }

    protected InputStreamUriParameterResolver getResolver(Transformer transformer) {
        URIResolver resolver = transformer.getURIResolver();
        SourceUriResolverChain resolverChain;
        if (!(resolver instanceof SourceUriResolverChain)) {
            resolverChain = new SourceUriResolverChain(resolver);
            resolverChain.addResolver(new InputStreamUriParameterResolver());
            resolverChain.addResolver(new ClassPathUriResolver());
            transformer.setURIResolver(resolverChain);
        } else {
            resolverChain = (SourceUriResolverChain) resolver;
        }

        return resolverChain.getResolverOfType(InputStreamUriParameterResolver.class);

    }

    protected void setInputParameters(String id, Transformer transformer, List<XsltParameter> inputs) {

        transformer.clearParameters();
        if (id == null) {
            return;
        }

        if (inputs != null && inputs.size() > 0) {
            com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver resolver = getResolver(transformer);
            for (XsltParameter input : inputs) {
                if (!"*".equals(input.getStyleId()) && !id.equals(input.getStyleId())) {
                    continue;
                }

                String param;
                if (input.getValue() instanceof InputStream) {
                    param = resolver.addStream((InputStream) input.getValue());
                } else {
                    param = input.getValue().toString();
                }
                transformer.setParameter(input.getName(), param);
            }
        }

    }

    public void executeChain(InputStream in, OutputStream output, List<XsltParameter> inputs) throws XsltException {
        try {
            for (XmlFilterReference filter : chain.getFilters()) {
                // pass the input stream to all transforms as a param inputstream

                Transformer transformer;
                if (filter.getReader() instanceof net.sf.saxon.Filter) {
                    net.sf.saxon.Filter saxonFilter = (net.sf.saxon.Filter) filter.getReader();
                    transformer = saxonFilter.getTransformer();
                    setInputParameters(filter.getId(), transformer, inputs);
                } else if (filter.getReader() instanceof TrAXFilter) {
                    TrAXFilter traxFilter = (TrAXFilter)filter.getReader();
                    transformer = traxFilter.getTransformer();
                    setInputParameters(filter.getId(), transformer, inputs);
                } else {
                    LOG.debug("Unable to set stylesheet parameters.  Unsupported xml filter type used: " + filter.getReader().getClass().getCanonicalName());
                }
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

        XMLReader lastFilter = chain.getFilters().get(chain.getFilters().size() - 1).getReader();

        return new SAXSource(lastFilter, source);
    }

}
