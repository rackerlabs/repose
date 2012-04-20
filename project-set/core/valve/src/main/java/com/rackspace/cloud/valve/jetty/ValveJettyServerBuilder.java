package com.rackspace.cloud.valve.jetty;

import com.rackspace.papi.container.config.SslConfiguration;
import com.rackspace.papi.filter.ValvePowerFilter;
import com.rackspace.cloud.valve.jetty.servlet.ProxyServlet;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.service.context.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;

import java.util.ArrayList;

import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValveJettyServerBuilder {

   private static final Logger LOG = LoggerFactory.getLogger(ValveJettyServerBuilder.class);

   private final List<Port> ports = new ArrayList<Port>();
   private String configurationPathAndFile = "";
   private final SslConfiguration sslConfiguration;

   public ValveJettyServerBuilder(String configurationPathAndFile, List<Port> ports, SslConfiguration sslConfiguration) {
      this.ports.addAll(ports);
      this.configurationPathAndFile = configurationPathAndFile;
      this.sslConfiguration = sslConfiguration;
   }

   public Server newServer() {

      Server server = new Server();
      List<Connector> connectors = new ArrayList<Connector>();

      for (Port p : ports) {
         if ("http".equalsIgnoreCase(p.getProtocol())) {
            connectors.add(createHttpConnector(p));
         } else if ("https".equalsIgnoreCase(p.getProtocol())) {
            connectors.add(createHttpsConnector(p));
         }
      }

      server.setConnectors(connectors.toArray(new Connector[connectors.size()]));

      final ServletContextHandler rootContext = buildRootContext(server);
      final ServletHolder valveServer = new ServletHolder(new ProxyServlet());

      rootContext.addFilter(new FilterHolder(ValvePowerFilter.class), "/*", EnumSet.allOf(DispatcherType.class));
      rootContext.addServlet(valveServer, "/*");

      server.setHandler(rootContext);

      return server;
   }

   private Connector createHttpConnector(Port port) {
      SelectChannelConnector connector = new SelectChannelConnector();
      connector.setPort(port.getPort());

      return connector;
   }

   private Connector createHttpsConnector(Port port) {
      SslSelectChannelConnector ssl_connector = new SslSelectChannelConnector();

      ssl_connector.setPort(port.getPort());
      SslContextFactory cf = ssl_connector.getSslContextFactory();

      cf.setKeyStore(configurationPathAndFile + "/" + sslConfiguration.getKeystoreFilename());
      cf.setKeyStorePassword(sslConfiguration.getKeystorePassword());
      cf.setKeyManagerPassword(sslConfiguration.getKeyPassword());

      return ssl_connector;
   }

   private ServletContextHandler buildRootContext(Server serverReference) {
      final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/");
      servletContext.getInitParams().put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), configurationPathAndFile);
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