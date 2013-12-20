package com.rackspace.papi.jetty;

public abstract class JettyTestingContext {

   private static final int DEFAULT_PORT = 26216;
   private JettyServerBuilder server;

   public JettyTestingContext() {
      server = null;
   }

   public final synchronized void standUp() throws JettyException {
      if (server == null) {
         server = new JettyServerBuilder(getPort());
         buildServerContext(server);

         server.start();
      }
   }

   public final synchronized void tearDown() throws JettyException {
      if (server != null && server.getServerInstance().isRunning()) {
         server.stop();
      }
   }

   public abstract void buildServerContext(JettyServerBuilder serverBuilder) throws JettyException;

   public static int getPort() {
      return DEFAULT_PORT;
   }
}
