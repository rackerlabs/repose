package com.rackspace.papi.components.translation.xproc.calabash;

import com.rackspace.papi.components.translation.util.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.xproc.Pipeline;
import com.rackspace.papi.components.translation.xproc.PipelineException;
import com.rackspace.papi.components.translation.xproc.PipelineInput;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.xml.sax.InputSource;

public class CalabashPipeline implements Pipeline {
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CalabashPipeline.class);
   private final InputStreamUriParameterResolver resolver;
   private final XProcRuntime runtime;
   private final XPipeline pipeline;
   private final boolean legacySourceOutput;
   
   public CalabashPipeline(XPipeline pipeline, XProcRuntime runtime, InputStreamUriParameterResolver resolver) {
      this(pipeline, runtime, resolver, false);
   }

   public CalabashPipeline(XPipeline pipeline, XProcRuntime runtime, InputStreamUriParameterResolver resolver, boolean legacySourceOutput) {
      this.resolver = resolver;
      this.runtime = runtime;
      this.pipeline = pipeline;
      this.legacySourceOutput = legacySourceOutput;
   }
   
   protected <T> void addParameter(PipelineInput<T> input) {
      RuntimeValue runtimeParam;
      T source = input.getSource();
      
      if (source instanceof InputStream) {
         runtimeParam =  new RuntimeValue(resolver.addStream((InputStream)source));
      } else if (source instanceof String) {
         runtimeParam =  new RuntimeValue(source.toString());
      } else {
         // TODO: handle other types?
         throw new IllegalArgumentException("Illegal parameter type: " + source.getClass().getName());
      }
      pipeline.setParameter(new QName("", input.getName()), runtimeParam);
   }
   
   protected <T> void addPort(PipelineInput<T> input) {
      XdmNode node;
      T source = input.getSource();
      
      if (source instanceof InputSource) {
         node = runtime.parse((InputSource)source);
      } else if (input.getSource() instanceof XdmNode) {
         node = (XdmNode) source;
      } else {
         // TODO: handle other types?
         throw new IllegalArgumentException("Illegal port type: " + source.getClass().getName());
      }
      
      pipeline.writeTo(input.getName(), node);
   }
   
   protected void addOption(PipelineInput input) {
      // TODO: Implement this
      throw new UnsupportedOperationException();
   }
   
   protected void handleInputs(List<PipelineInput> inputs) {
      for (PipelineInput input: inputs) {
         switch (input.getType()) {
            case PORT:
               addPort(input);
               break;
               
            case PARAMETER:
               addParameter(input);
               break;
               
            case OPTION:
               addOption(input);
               break;
               
            default:
               throw new IllegalArgumentException("Input type not supported: " + input.getType());
         }
      }
   }
   
   protected <T> void clearParameter(PipelineInput<T> input) {
      T source = input.getSource();
      
      if (source instanceof InputStream) {
         try {
            ((InputStream)source).close();
         } catch (IOException ex) {
            LOG.error("Unable to close input stream", ex);
         }
         resolver.removeStream((InputStream)source);
      }
   }
   
   protected void clearParameters(List<PipelineInput> inputs) {
      
      for (PipelineInput input: inputs) {
         switch (input.getType()) {
            case PARAMETER:
               clearParameter(input);
               break;
            default:
               break;
         }
      }
   }
   
   @Override
   public void reset() {
      pipeline.reset();
   }
      
   
   @Override
   public void run(List<PipelineInput> inputs) throws PipelineException {
      try {
         reset();
         handleInputs(inputs);
         try {
         
            pipeline.run();
            
         } finally {
            clearParameters(inputs);
         }
      } catch (SaxonApiException ex) {
         throw new PipelineException(ex);
      }
   }
   
   @Override
   public List<Source> getResultPort(String name) {
      List<Source> ret = null;

      if (!legacySourceOutput) {
         ret = getCalabashResultPort(name);
      } else {
         ret = getLegacyResultPort(name);
      }

      return ret;
   }

   protected List<Source> getCalabashResultPort(String name)
      throws PipelineException {
      try {
         ReadablePipe pipe = pipeline.readFrom(name);
         List<Source> nodes = new ArrayList<Source>();

         while(pipe.moreDocuments()) {
            nodes.add(pipe.read().asSource());
         }

         return nodes;
      } catch (SaxonApiException ex) {
         throw new PipelineException(ex);
      }
   }

   protected List<Source> getLegacyResultPort(String name)
      throws PipelineException {
      try {
         List<Source> standard = getCalabashResultPort (name);
         List<Source> ret = new ArrayList<Source>(standard.size());

         TransformerFactory transFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl",null);
         Transformer transformer = transFactory.newTransformer();

         for (Source s : standard) {
            DOMResult result = new DOMResult();
            transformer.transform (s, result);
            ret.add (new DOMSource (result.getNode()));
         }

         return ret;
      }catch (TransformerConfigurationException tce) {
         throw new PipelineException (tce);
      }catch (TransformerException te) {
         throw new PipelineException (te);
      }
   }
}
