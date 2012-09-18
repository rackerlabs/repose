package com.rackspace.papi.components.translation.xslt.handlerchain;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.OutputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import com.rackspace.papi.components.translation.xslt.Parameter;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltException;
import java.io.*;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
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
    private final SAXTransformerFactory factory;

    public XsltHandlerChainExecutor(SAXTransformerFactory factory, XsltHandlerChain chain) {
        this.factory = factory;
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

    public void executeChain(InputStream in, OutputStream out, List<Parameter> inputs, List<Parameter<? extends OutputStream>> outputs) throws XsltException {
        try {
            XMLReader reader = getSaxReader();
            // TODO: Make validation optional
            reader.setFeature("http://xml.org/sax/features/validation", false);

            if (!chain.getFilters().isEmpty()) {

                TransformerHandler lastHandler = null;
                
                for (TransformReference<Templates> template : chain.getFilters()) {
                    TransformerHandler handler = factory.newTransformerHandler(template.getFilter());
                    Transformer transformer = handler.getTransformer();
                    transformer.clearParameters();
                    
                    if (lastHandler == null) {
                        reader.setContentHandler(handler);
                    } else {
                        lastHandler.setResult(new SAXResult(handler));
                    }

                    //transformer.setURIResolver(new ClassPathUriResolver(transformer.getURIResolver()));
                    setInputParameters(template.getId(), transformer, inputs);
                    setAlternateOutputs(template.getId(), transformer, outputs);
                    lastHandler = handler;
                }

                // Set the result of the last handler to be the output stream
                lastHandler.setResult(new StreamResult(out));
            } else {
                TransformerHandler handler = factory.newTransformerHandler();
                reader.setContentHandler(handler);
                handler.setResult(new StreamResult(out));
            }

            reader.parse(new InputSource(in));
            //in.close();
        } catch (TransformerConfigurationException ex) {
            throw new XsltException(ex);
        } catch (IOException ex) {
            throw new XsltException(ex);
        } catch (ParserConfigurationException ex) {
            throw new XsltException(ex);
        } catch (SAXException ex) {
            throw new XsltException(ex);
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
