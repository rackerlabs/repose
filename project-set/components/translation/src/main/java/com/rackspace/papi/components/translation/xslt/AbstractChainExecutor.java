package com.rackspace.papi.components.translation.xslt;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.OutputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;
import net.sf.saxon.Controller;
import net.sf.saxon.lib.OutputURIResolver;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class AbstractChainExecutor {

    private final SAXTransformerFactory factory;

    public AbstractChainExecutor() {
        this.factory = null;
    }

    public AbstractChainExecutor(SAXTransformerFactory factory) {
        this.factory = factory;
    }

    public SAXTransformerFactory getFactory() {
        return factory;
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
