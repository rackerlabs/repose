package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.components.translation.resolvers.ClassPathUriResolver;
import com.rackspace.papi.components.translation.resolvers.OutputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.resolvers.SourceUriResolverChain;
import com.rackspace.papi.components.translation.xslt.Parameter;
import com.rackspace.papi.components.translation.xslt.TransformReference;
import com.rackspace.papi.components.translation.xslt.XsltException;
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
import net.sf.saxon.Controller;
import net.sf.saxon.lib.OutputURIResolver;
import org.xml.sax.InputSource;

public class XsltFilterChainExecutor {

   private final XsltFilterChain chain;
   private final Properties format = new Properties();

   public XsltFilterChainExecutor(XsltFilterChain chain) {
      this.chain = chain;
      format.put(OutputKeys.METHOD, "xml");
      format.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
      format.put(OutputKeys.ENCODING, "UTF-8");
      format.put(OutputKeys.INDENT, "yes");
   }

    private com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver getResolver(Transformer transformer) {
        URIResolver resolver = transformer.getURIResolver();
        SourceUriResolverChain resolverChain;
        if (!(resolver instanceof SourceUriResolverChain)) {
            resolverChain = new SourceUriResolverChain(resolver);
            resolverChain.addResolver(new com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver());
            resolverChain.addResolver(new ClassPathUriResolver());
            transformer.setURIResolver(resolverChain);
        } else {
            resolverChain = (SourceUriResolverChain) resolver;
        }

        return resolverChain.getResolverOfType(com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver.class);

    }

    private void setInputParameters(String id, Transformer transformer, List<Parameter> inputs) {
        
        transformer.clearParameters();

        if (inputs != null && inputs.size() > 0) {
            com.rackspace.papi.components.translation.resolvers.InputStreamUriParameterResolver resolver = getResolver(transformer);
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

   public void executeChain(InputStream in, OutputStream output, List<Parameter> inputs, List<Parameter<? extends OutputStream>> outputs) throws XsltException {
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

      return new SAXSource(chain.getFilters().get(chain.getFilters().size() - 1).getFilter(), source);
   }
}
