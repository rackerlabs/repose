package com.rackspace.papi.components.datastore.integration;

import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.filter.PowerFilter;
import com.rackspace.papi.service.context.PowerApiContextManager;
import com.rackspace.papi.jetty.JettyServerBuilder;
import com.rackspace.papi.jetty.JettyTestingContext;
import com.rackspace.papi.test.DummyServlet;

public class StandAloneDatastoreServer extends JettyTestingContext {

   public static void main(String args[]) {
      try {
         new StandAloneDatastoreServer(2101);
//         new StandAloneDatastoreServer(2102);
//         new StandAloneDatastoreServer(2103);
//         new StandAloneDatastoreServer(2104);
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }

   public StandAloneDatastoreServer(int port) throws Exception {
      final JettyServerBuilder server = new JettyServerBuilder(port);
      buildServerContext(server);
      server.start();

      System.out.println("Server started");
   }

   @Override
   public final void buildServerContext(JettyServerBuilder serverBuilder) throws Exception {
      serverBuilder.addContextListener(PowerApiContextManager.class);
      serverBuilder.addContextInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), "/home/zinic/ulocal/local/etc/powerapi/test");
      serverBuilder.addFilter(PowerFilter.class, "/*");
      serverBuilder.addServlet(DummyServlet.class, "/*");
   }
}
