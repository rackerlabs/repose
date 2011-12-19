package com.rackspace.papi.components.translation.xproc;

import com.rackspace.papi.components.translation.util.InputStreamUriParameterResolver;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractPipeline implements Pipeline {
   private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractPipeline.class);
   private final InputStreamUriParameterResolver resolver;
   
   public AbstractPipeline(InputStreamUriParameterResolver resolver) {
      this.resolver = resolver;
   }
   
   protected abstract <T> void addParameter(PipelineInput<T> input);
   
   protected abstract <T>void addPort(PipelineInput<T> input);
   
   protected abstract <T>void addOption(PipelineInput<T> input);
   
   protected void handleInputs(PipelineInput... inputs) {
      handleInputs(Arrays.asList(inputs));
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
   
   protected InputStreamUriParameterResolver getUriResolver() {
      return resolver;
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
   
   protected void clearParameters(PipelineInput... inputs) {
      clearParameters(Arrays.asList(inputs));
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
   public void run(PipelineInput... inputs) {
      run(Arrays.asList(inputs));
   }
   
}
