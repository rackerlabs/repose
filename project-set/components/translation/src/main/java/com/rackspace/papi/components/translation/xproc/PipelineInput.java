package com.rackspace.papi.components.translation.xproc;

public class PipelineInput<T> {
   private final String name;
   private final PipelineInputType type;
   private final T source;
   
   public static <P> PipelineInput parameter(String name, P source) {
      return new PipelineInput<P>(name, PipelineInputType.PARAMETER, source);
   }
   
   public static <P> PipelineInput port(String name, P source) {
      return new PipelineInput<P>(name, PipelineInputType.PORT, source);
   }
   
   public static <P> PipelineInput option(String name, P source) {
      return new PipelineInput<P>(name, PipelineInputType.OPTION, source);
   }

   public PipelineInput(String name, PipelineInputType type, T source) {
      this.name = name;
      this.type = type;
      this.source = source;
   }
   
   public String getName() {
      return name;
   }
   
   public T getSource() {
      return source;
   }
   
   public PipelineInputType getType() {
      return type;
   }
   
}
