package com.rackspace.papi.components.translation.xproc.calabash;

import com.rackspace.papi.components.translation.util.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.xproc.AbstractPipeline;
import com.rackspace.papi.components.translation.xproc.Pipeline;
import com.rackspace.papi.components.translation.xproc.PipelineException;
import com.rackspace.papi.components.translation.xproc.PipelineInput;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;
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

public class CalabashPipeline extends AbstractPipeline implements Pipeline {
   private final XProcRuntime runtime;
   private final XPipeline pipeline;
   private final boolean legacySourceOutput;
   
   public CalabashPipeline(XPipeline pipeline, XProcRuntime runtime, InputStreamUriParameterResolver resolver) {
      this(pipeline, runtime, resolver, false);
   }

   public CalabashPipeline(XPipeline pipeline, XProcRuntime runtime, InputStreamUriParameterResolver resolver, boolean legacySourceOutput) {
      super(resolver);
      this.runtime = runtime;
      this.pipeline = pipeline;
      this.legacySourceOutput = legacySourceOutput;
   }
   
   protected <T> RuntimeValue getRuntimeValue(PipelineInput<T> input) {
      RuntimeValue runtimeValue;
      T source = input.getSource();
      
      if (source instanceof InputStream) {
         runtimeValue =  new RuntimeValue(getUriResolver().addStream((InputStream)source));
      } else if (source instanceof String) {
         runtimeValue =  new RuntimeValue(source.toString());
      } else {
         // TODO: handle other types?
         throw new IllegalArgumentException("Illegal input type: " + source.getClass().getName());
      }
      
      return runtimeValue;
   }
   
   @Override
   protected <T> void addParameter(PipelineInput<T> input) {
      RuntimeValue runtimeParam = getRuntimeValue(input);
      
      if (pipeline.getInputs().contains("parameters")) {
         pipeline.setParameter("parameters", new QName("", input.getName()), runtimeParam);
      } else {
         pipeline.setParameter(new QName("", input.getName()), runtimeParam);
      }
   }
   
   @Override
   protected void addOption(PipelineInput input) {
      pipeline.setOption(new QName(input.getName()), getRuntimeValue(input));
   }
   
   @Override
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
   
   @Override
   public void reset() {
      pipeline.reset();
   }
      
   
   @Override
   public void run(List<PipelineInput> inputs) {
      try {
         reset();
         handleInputs(inputs);
         try {
         
            pipeline.run();
            
         } finally {
            clearParameters(inputs);
         }
      } catch (SaxonApiException ex) {
         // TODO: Should we log the exception here?
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

   protected List<Source> getCalabashResultPort(String name) {
      try {
         ReadablePipe pipe = pipeline.readFrom(name);
         List<Source> nodes = new ArrayList<Source>();

         while(pipe.moreDocuments()) {
            nodes.add(pipe.read().asSource());
         }

         return nodes;
      } catch (SaxonApiException ex) {
         // TODO: Should we log the exception here?
         throw new PipelineException(ex);
      }
   }

   protected List<Source> getLegacyResultPort(String name) {
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
         // TODO: Should we log the exception here?
         throw new PipelineException (tce);
      }catch (TransformerException te) {
         // TODO: Should we log the exception here?
         throw new PipelineException (te);
      }
   }
}
