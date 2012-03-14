package com.rackspace.papi.components.translation.xproc.calabash;

import com.rackspace.papi.components.translation.util.ClassPathUriResolver;
import com.rackspace.papi.components.translation.util.InputStreamUriParameterResolver;
import com.rackspace.papi.components.translation.xproc.Pipeline;
import com.rackspace.papi.components.translation.xproc.PipelineBuilder;
import com.rackspace.papi.components.translation.xproc.PipelineException;
import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.XProcURIResolver;
import javax.xml.transform.URIResolver;
import net.sf.saxon.s9api.SaxonApiException;

public class CalabashPipelineBuilder implements PipelineBuilder {
   private final boolean schemaAware;
   private final boolean legacySourceOutput;
   
   public CalabashPipelineBuilder() {
      this(true, false);
   }
   
   public CalabashPipelineBuilder(boolean schemaAware) {
      this(schemaAware, false);
   }

   public CalabashPipelineBuilder(boolean schemaAware, boolean legacySourceOutput) {
      this.schemaAware = schemaAware;
      this.legacySourceOutput = legacySourceOutput;
   }
   
   @Override
   public Pipeline build(String pipelineUri) {
      try {
         XProcConfiguration config = new XProcConfiguration(schemaAware);
         XProcRuntime runtime = new XProcRuntime(config);
         InputStreamUriParameterResolver resolver = new InputStreamUriParameterResolver(new XProcURIResolver(runtime));
         resolver.addResolver(new ClassPathUriResolver());
         runtime.setURIResolver(resolver);
         XPipeline pipeline = runtime.load(pipelineUri);
         return new CalabashPipeline(pipeline, runtime, resolver, legacySourceOutput);
      } catch (SaxonApiException ex) {
         // TODO: Should we log the exception here?
         throw new PipelineException(ex);
      }
   }
   
   @Override
   public Pipeline build(String pipelineUri, URIResolver... resolvers) {
      try {
         XProcConfiguration config = new XProcConfiguration(schemaAware);
         XProcRuntime runtime = new XProcRuntime(config);
         InputStreamUriParameterResolver streamResolver = new InputStreamUriParameterResolver(new XProcURIResolver(runtime));
         streamResolver.addResolver(new ClassPathUriResolver());
         for (URIResolver resolver: resolvers) {
            streamResolver.addResolver(resolver);
         }
         runtime.setURIResolver(streamResolver);
         XPipeline pipeline = runtime.load(pipelineUri);
         return new CalabashPipeline(pipeline, runtime, streamResolver);
      } catch (SaxonApiException ex) {
         // TODO: Should we log the exception here?
         throw new PipelineException(ex);
      }
   }
}
