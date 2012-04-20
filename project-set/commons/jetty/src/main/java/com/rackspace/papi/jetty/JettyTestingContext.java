package com.rackspace.papi.jetty;

public abstract class JettyTestingContext {

   private JettyServerBuilder server;

   public JettyTestingContext() {
      server = null;
   }

   public final synchronized void standUp() throws Exception {
      if (server == null) {
         server = new JettyServerBuilder(getPort());
         buildServerContext(server);

         server.start();
      }
   }

   public final synchronized void tearDown() throws Exception {
      if (server != null && server.getServerInstance().isRunning()) {
         server.stop();
      }
   }

   public abstract void buildServerContext(JettyServerBuilder serverBuilder) throws Exception;

   public static int getPort() {
      return 26216;
   }
}
