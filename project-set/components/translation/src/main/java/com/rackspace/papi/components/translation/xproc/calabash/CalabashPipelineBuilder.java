package com.rackspace.papi.components.translation.xproc.calabash;

import com.rackspace.papi.components.translation.util.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.xproc.Pipeline;
import com.rackspace.papi.components.translation.xproc.PipelineBuilder;
import com.rackspace.papi.components.translation.xproc.PipelineException;
import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.XProcURIResolver;
import net.sf.saxon.s9api.SaxonApiException;

public class CalabashPipelineBuilder implements PipelineBuilder {
   private final boolean schemaAware;
   
   public CalabashPipelineBuilder() {
      this(true);
   }
   
   public CalabashPipelineBuilder(boolean schemaAware) {
      this.schemaAware = schemaAware;
   }
   
   @Override
   public Pipeline build(String pipelineUri) {
      try {
         XProcConfiguration config = new XProcConfiguration(schemaAware);
         XProcRuntime runtime = new XProcRuntime(config);
         XPipeline pipeline = runtime.load(pipelineUri);
         InputStreamUriParameterResolver resolver = new InputStreamUriParameterResolver(new XProcURIResolver(runtime));
         runtime.setURIResolver(resolver);
         return new CalabashPipeline(pipeline, runtime, resolver);
      } catch (SaxonApiException ex) {
         throw new PipelineException(ex);
      }
   }
   
}
