package com.rackspace.cloud.valve.jetty;

import com.rackspace.papi.filter.ValvePowerFilter;
import com.rackspace.cloud.valve.jetty.servlet.ProxyServlet;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.service.context.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;
import java.util.ArrayList;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.List;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValveJettyServerBuilder {

   private static final Logger LOG = LoggerFactory.getLogger(ValveJettyServerBuilder.class);
   private final Integer httpPortNumber;
   private final Integer httpsPortNumber;
   private String configurationPathAndFile = "";

   public ValveJettyServerBuilder(Integer httpPortNumber, Integer httpsPortNumber, String configurationPathAndFile) {
      this.httpPortNumber = httpPortNumber;
      this.httpsPortNumber = httpsPortNumber;
      this.configurationPathAndFile = configurationPathAndFile;
   }

   public Server newServer() {
      //return buildWarContext(new Server(httpPortNumber));

      // TODO: add https
      Server server = new Server(httpPortNumber);
      final ServletContextHandler rootContext = buildRootContext(server);
      final ServletHolder valveServer = new ServletHolder(new ProxyServlet());

      rootContext.addFilter(new FilterHolder(ValvePowerFilter.class), "/*", EnumSet.allOf(DispatcherType.class));
      rootContext.addServlet(valveServer, "/*");

      return server;
   }

   private Server buildWarContext(Server server) {
      // Load ROOT.war from an external location
      String ROOT = configurationPathAndFile + "/ROOT.war";
      WebAppContext context = new WebAppContext(ROOT, "/");
      
      // Load valve.jar as a "executable" war
      //URL ROOT = ValveJettyServerBuilder.class.getProtectionDomain().getCodeSource().getLocation();
      //WebAppContext context = new WebAppContext(ROOT.toExternalForm(), "/");
      
      List<Port> ports = new ArrayList<Port>();
      ports.add(new Port("http", httpPortNumber));

      if (httpsPortNumber != null) {
         ports.add(new Port("https", httpsPortNumber));
      }
      
      context.getAttributes().setAttribute(InitParameter.PORT.getParameterName(), ports);
      context.getInitParams().put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), configurationPathAndFile);
      context.setServer(server);
      server.setHandler(context);
      return server;
   }

   private ServletContextHandler buildRootContext(Server serverReference) {
      final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/");
      servletContext.getInitParams().put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), configurationPathAndFile);

      List<Port> ports = new ArrayList<Port>();
      ports.add(new Port("http", httpPortNumber));

      if (httpsPortNumber != null) {
         ports.add(new Port("https", httpsPortNumber));
      }
      
      servletContext.getAttributes().setAttribute(InitParameter.PORT.getParameterName(), ports);

      try {
         servletContext.addEventListener(PowerApiContextManager.class.newInstance());
      } catch (InstantiationException e) {
         throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
      } catch (IllegalAccessException e) {
         throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
      }

      return servletContext;
   }
}