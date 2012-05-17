package com.rackspace.papi.service.context;

import com.rackspace.papi.domain.Port;
import com.rackspace.papi.servlet.InitParameter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public final class ServletContextHelper {

   public static final String SERVLET_CONTEXT_ATTRIBUTE_NAME = "PAPI_ServletContext";
   public static final String SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME = "PAPI_SpringApplicationContext";
   private static final Logger LOG = LoggerFactory.getLogger(ServletContextHelper.class);
   private static final Object lock = new Object();
   private static ServletContextHelper instance = null;
   private final ContextAdapterProvider adapterProvider;

   public static ServletContextHelper configureInstance(ContextAdapterProvider adapterProvider, ServletContext ctx, ApplicationContext applicationContext) {
      synchronized (lock) {
         if (adapterProvider != null) {
            LOG.debug("Configuring ContextAdapterProvider: " + adapterProvider.getClass().getName());
            instance = new ServletContextHelper(adapterProvider);
            instance.setPowerApiContext(ctx, applicationContext);
         }
         return instance;
      }
   }

   public static ServletContextHelper getInstance() {
      synchronized (lock) {
         return instance;
      }
   }

   private ServletContextHelper() {
      this.adapterProvider = null;
   }

   private ServletContextHelper(ContextAdapterProvider adapterProvider) {
      this.adapterProvider = adapterProvider;
   }

   public ApplicationContext getApplicationContext(ServletContext ctx) {
      return (ApplicationContext) ctx.getAttribute(SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME);
   }

   public ContextAdapter getPowerApiContext(ServletContext ctx) {
      return adapterProvider.newInstance(null);
   }

   public void setPowerApiContext(ServletContext ctx, ApplicationContext applicationContext) {
      ctx.setAttribute(SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME, applicationContext);
   }

   public List<Port> getServerPorts(ServletContext ctx) {
      Object port = ctx.getAttribute(InitParameter.PORT.getParameterName());

      if (port != null) {
         return (List<Port>) ctx.getAttribute(InitParameter.PORT.getParameterName());
      } else {
         return new ArrayList<Port>();
      }
   }
}
