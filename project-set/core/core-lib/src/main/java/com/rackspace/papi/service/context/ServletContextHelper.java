package com.rackspace.papi.service.context;

import com.rackspace.papi.domain.Port;
import com.rackspace.papi.servlet.InitParameter;
import java.util.ArrayList;
import java.util.List;
import javax.naming.Context;
import javax.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServletContextHelper {

   public static final String SERVLET_CONTEXT_ATTRIBUTE_NAME = "PAPI_ServletContext";
   private static final Logger LOG = LoggerFactory.getLogger(ServletContextHelper.class);
   private static final Object lock = new Object();
   private static ServletContextHelper instance = null;
   private final ContextAdapterProvider adapterProvider;

   public static void configureInstance(ContextAdapterProvider adapterProvider, ServletContext ctx, Context namingContext) {
      synchronized (lock) {
         if (instance == null && adapterProvider != null) {
            LOG.debug("Configuring ContextAdapterProvider: " + adapterProvider.getClass().getName());
            instance = new ServletContextHelper(adapterProvider);
            instance.setPowerApiContext(ctx, namingContext);
         }
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

   public Context namingContext(ServletContext ctx) {
      final Object o = ctx.getAttribute(SERVLET_CONTEXT_ATTRIBUTE_NAME);

      if (o == null) {
         throw new IllegalArgumentException("Servlet Context attribute \""
                 + SERVLET_CONTEXT_ATTRIBUTE_NAME
                 + "\" appears to not be set. Has the PowerApiContextManager been set as a servlet context listener");
      }

      if (!(o instanceof Context)) {
         throw new IllegalStateException("Servlet Context attribute \""
                 + SERVLET_CONTEXT_ATTRIBUTE_NAME
                 + "\" is not a valid jndi naming context.");
      }

      return (Context) o;
   }

   public ContextAdapter getPowerApiContext(ServletContext ctx) {
      return adapterProvider.newInstance(namingContext(ctx));
   }

   public void setPowerApiContext(ServletContext ctx, Context namingContext) {
      ctx.setAttribute(SERVLET_CONTEXT_ATTRIBUTE_NAME, namingContext);
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
