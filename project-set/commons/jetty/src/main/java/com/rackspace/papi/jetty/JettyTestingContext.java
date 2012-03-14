package com.rackspace.papi.jetty;

public abstract class JettyTestingContext {

   private JettyServerBuilder server;

   public JettyTestingContext() {
      server = null;
   }

   public synchronized final void standUp() throws Exception {
      if (server == null) {
         server = new JettyServerBuilder(getPort());
         buildServerContext(server);

         server.start();
      }
   }

   public synchronized final void tearDown() throws Exception {
      if (server != null && server.getServerInstance().isRunning()) {
         server.stop();
      }
   }

   public abstract void buildServerContext(JettyServerBuilder serverBuilder) throws Exception;

   public static int getPort() {
      return 26216;
   }
}
