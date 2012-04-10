package com.rackspace.papi.service.context.jndi;

import com.rackspace.papi.domain.Port;
import com.rackspace.papi.servlet.InitParameter;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.servlet.ServletContext;

public final class ServletContextHelper {

   public static final String SERVLET_CONTEXT_ATTRIBUTE_NAME = "PAPI_ServletContext";

   private ServletContextHelper() {
   }

   public static Context namingContext(ServletContext ctx) {
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

   public static ContextAdapter getPowerApiContext(ServletContext ctx) {
      return new JndiContextAdapter(namingContext(ctx));
   }

   public static void setPowerApiContext(ServletContext ctx, Context namingContext) {
      ctx.setAttribute(SERVLET_CONTEXT_ATTRIBUTE_NAME, namingContext);
   }

   public static List<Port> getServerPorts(ServletContext ctx) {
      Object port = ctx.getAttribute(InitParameter.PORT.getParameterName());

      if (port != null) {
         return (List<Port>) ctx.getAttribute(InitParameter.PORT.getParameterName());
      } else {
         return new ArrayList<Port>();
      }
   }
}
