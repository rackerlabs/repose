package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.OutputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.saxon.Controller;
import net.sf.saxon.lib.OutputURIResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XmlFilterChainExecutor {

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

    protected OutputStreamUriParameterResolver getOutputResolver(Controller controller) {
        OutputURIResolver currentResolver = controller.getOutputURIResolver();
        OutputStreamUriParameterResolver outputResolver;

        if (currentResolver instanceof OutputStreamUriParameterResolver) {
            outputResolver = (OutputStreamUriParameterResolver) currentResolver;
            outputResolver.clearStreams();
        } else {
            outputResolver = new OutputStreamUriParameterResolver(currentResolver);
        }

        controller.setOutputURIResolver(outputResolver);

        return outputResolver;
    }

    protected void setAlternateOutputs(String id, Transformer transformer, List<XsltParameter<? extends OutputStream>> outputs) {
        if (outputs != null && outputs.size() > 0) {
            if (transformer instanceof Controller) {
                OutputStreamUriParameterResolver outputResolver = getOutputResolver((Controller) transformer);

                for (XsltParameter<? extends OutputStream> output : outputs) {
                    outputResolver.addStream(output.getValue(), output.getName());
                }
            }
        }
    }

    public void executeChain(InputStream in, OutputStream output, List<XsltParameter> inputs, List<XsltParameter<? extends OutputStream>> outputs) throws XsltException {
        try {
            for (XmlFilterReference filter : chain.getFilters()) {
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

    protected XMLReader getSaxReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(true);
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        if (reader == null) {
            reader = XMLReaderFactory.createXMLReader();
        }

        return reader;
    }
}
