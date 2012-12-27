package com.rackspace.papi.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;
import java.util.EnumSet;

public class JettyServerBuilder {

   private static final Logger LOG = LoggerFactory.getLogger(JettyServerBuilder.class);
   private final ServletContextHandler rootContext;
   private final Server server;

   public JettyServerBuilder(int portNumber) {
      server = new Server(portNumber);
      rootContext = new ServletContextHandler(server, "/");
   }

   Server getServerInstance() {
      return server;
   }

   public ServletContextHandler getServletContextHandler() {
      return rootContext;
   }

          public void addContextInitParameter(String name, String value) {
              rootContext.getInitParams().put(name, value);
          }

       public void addContextListener(Class<? extends ServletContextListener> contextListener)
       throws IllegalAccessException, InstantiationException {
           rootContext.addEventListener(contextListener.newInstance());
       }

       public FilterHolder addFilter(Class<? extends Filter> filterClass, String pathspec) {
           final FilterHolder filterInstasnce = new FilterHolder(filterClass);
           rootContext.addFilter(filterInstasnce, pathspec, EnumSet.allOf(DispatcherType.class));

           return filterInstasnce;
       }

       public ServletHolder addServlet(Class<? extends Servlet> servletClass, String pathspec) {
           final ServletHolder servletInstance = new ServletHolder(servletClass);
           rootContext.addServlet(servletInstance, pathspec);

           return servletInstance;
       }

       public void start() throws JettyException {
           try {
         server.start();
      } catch (Exception ex) {
         LOG.error("error occurred in start", ex);
         throw new JettyException("Error starting Jetty", ex);
      }
   }

   public void stop() throws JettyException {
      try {
         server.stop();
      } catch (Exception ex) {
         LOG.error("error occurred in stop", ex);
         throw new JettyException("Error stopping Jetty", ex);
      }
   }
}
