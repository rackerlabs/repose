package com.rackspace.papi.components.translation.xslt.handlerchain;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.OutputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import java.io.*;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import net.sf.saxon.Controller;
import net.sf.saxon.lib.OutputURIResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XsltHandlerChainExecutor {

    private final XsltHandlerChain chain;

    public XsltHandlerChainExecutor(XsltHandlerChain chain) {
        this.chain = chain;
    }

    private InputStreamUriParameterResolver getResolver(Transformer transformer) {
        URIResolver resolver = transformer.getURIResolver();
        SourceUriResolverChain resolverChain;
        if (resolver == null || !(resolver instanceof SourceUriResolverChain)) {
            resolverChain = new SourceUriResolverChain(resolver);
            resolverChain.addResolver(new InputStreamUriParameterResolver());
            resolverChain.addResolver(new ClassPathUriResolver());
            transformer.setURIResolver(resolverChain);
        } else {
            resolverChain = (SourceUriResolverChain) resolver;
        }

        return resolverChain.getResolverOfType(InputStreamUriParameterResolver.class);

    }

    private void setInputParameters(String id, Transformer transformer, List<Parameter> inputs) {

        if (inputs != null && inputs.size() > 0) {
            InputStreamUriParameterResolver resolver = getResolver(transformer);
            for (Parameter input : inputs) {
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

    private OutputStreamUriParameterResolver getOutputResolver(Controller controller) {
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

    private void setAlternateOutputs(String id, Transformer transformer, List<Parameter<? extends OutputStream>> outputs) {
        if (outputs != null && outputs.size() > 0) {
            if (transformer instanceof Controller) {
                OutputStreamUriParameterResolver outputResolver = getOutputResolver((Controller) transformer);

                for (Parameter<? extends OutputStream> output : outputs) {
                    outputResolver.addStream(output.getValue(), output.getName());
                }
            }
        }
    }

    public void executeChain(InputStream in, OutputStream out, List<Parameter> inputs, List<Parameter<? extends OutputStream>> outputs) throws XsltHandlerException {
        try {
            XMLReader reader = getSaxReader();
            // TODO: Make validation optional
            reader.setFeature("http://xml.org/sax/features/validation", false);

            if (!chain.getHandlers().isEmpty()) {

                // Set the content handler of the reader to be the first handler
                XslTransformer firstHandler = chain.getHandlers().get(0);
                reader.setContentHandler(firstHandler.getHandler());

                for (XslTransformer handler : chain.getHandlers()) {
                    Transformer transformer = handler.getHandler().getTransformer();
                    transformer.clearParameters();

                    //transformer.setURIResolver(new ClassPathUriResolver(transformer.getURIResolver()));
                    setInputParameters(handler.getId(), transformer, inputs);
                    setAlternateOutputs(handler.getId(), transformer, outputs);
                }

                // Set the result of the last handler to be the output stream
                chain.getHandlers().get(chain.getHandlers().size() - 1).getHandler().setResult(new StreamResult(out));
            }

            reader.parse(new InputSource(in));
            //in.close();
        } catch (IOException ex) {
            throw new XsltHandlerException(ex);
        } catch (ParserConfigurationException ex) {
            throw new XsltHandlerException(ex);
        } catch (SAXException ex) {
            throw new XsltHandlerException(ex);
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
