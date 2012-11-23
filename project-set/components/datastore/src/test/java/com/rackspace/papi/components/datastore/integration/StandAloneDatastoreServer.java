package com.rackspace.papi.components.datastore.integration;

import com.rackspace.papi.filter.PowerFilter;
import com.rackspace.papi.jetty.JettyException;
import com.rackspace.papi.jetty.JettyServerBuilder;
import com.rackspace.papi.jetty.JettyTestingContext;
import com.rackspace.papi.service.context.impl.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.test.DummyServlet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StandAloneDatastoreServer extends JettyTestingContext {

   public static void main(String args[]) {
      try {
         new StandAloneDatastoreServer(2101, "/home/zinic/ulocal/local/etc/powerapi/dist-datastore/node1");
         new StandAloneDatastoreServer(2102, "/home/zinic/ulocal/local/etc/powerapi/dist-datastore/node2");
         new StandAloneDatastoreServer(2103, "/home/zinic/ulocal/local/etc/powerapi/dist-datastore/node3");
         new StandAloneDatastoreServer(2104, "/home/zinic/ulocal/local/etc/powerapi/dist-datastore/node4");
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }

   private final String cfgPath;
   
   public StandAloneDatastoreServer(int port, String cfgPath) throws JettyException {
      this.cfgPath = cfgPath;
      
      final JettyServerBuilder server = new JettyServerBuilder(port);
      buildServerContext(server);
      server.start();

      System.out.println("Server started");
   }

   @Override
   public final void buildServerContext(JettyServerBuilder serverBuilder) throws JettyException {
        try {
            serverBuilder.addContextListener(PowerApiContextManager.class);
        } catch (IllegalAccessException ex) {
            throw new JettyException(ex);
        } catch (InstantiationException ex) {
            throw new JettyException(ex);
        }
      serverBuilder.addContextInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), cfgPath);
      serverBuilder.addFilter(PowerFilter.class, "/*");
      serverBuilder.addServlet(DummyServlet.class, "/*");
   }
}
