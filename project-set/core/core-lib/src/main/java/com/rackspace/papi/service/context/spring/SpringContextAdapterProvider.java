package com.rackspace.papi.service.context.spring;

import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ContextAdapterProvider;
import org.springframework.context.ApplicationContext;

import javax.naming.Context;

public class SpringContextAdapterProvider implements ContextAdapterProvider {
   private final ApplicationContext applicationContext;

   public SpringContextAdapterProvider(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
   }
   
   @Override
   public ContextAdapter newInstance(Context context) {
      return new SpringContextAdapter(applicationContext);
   }
   
}
