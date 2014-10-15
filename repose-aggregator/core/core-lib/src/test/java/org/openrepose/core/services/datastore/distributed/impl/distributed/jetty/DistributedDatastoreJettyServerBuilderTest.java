package org.openrepose.core.services.datastore.distributed.impl.distributed.jetty;

import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.services.datastore.DatastoreService;
import org.openrepose.services.datastore.impl.DatastoreServiceImpl;
import org.openrepose.core.services.datastore.distributed.impl.distributed.servlet.DistributedDatastoreServletContextManager;
import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class DistributedDatastoreJettyServerBuilderTest {

   public static class WhenCreatingDistributedDatastoreBuilder {

      private DatastoreService datastoreService;
      private ReposeInstanceInfo instanceInfo;
      
      @Before
      public void setUp() {
         
         datastoreService = new DatastoreServiceImpl();
         instanceInfo = new ReposeInstanceInfo("repose", "node1");
      }
      
      @Test
      public void shouldCreateNewServer() throws IllegalAccessException, InstantiationException {
         
         DistributedDatastoreJettyServerBuilder builder = new DistributedDatastoreJettyServerBuilder(8888, instanceInfo, "/etc/repose",
                 DistributedDatastoreServletContextManager.class.newInstance());
         final Server server = builder.newServer(datastoreService, instanceInfo);
         
         assertTrue(server instanceof Server);
         
      }
   }
}
