package com.rackspace.papi.service.datastore.impl.distributed.jetty;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.impl.PowerApiDatastoreService;
import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;


@RunWith(Enclosed.class)
public class DistributedDatastoreLauncherServiceImplTest {

   public static class WhenStoppingDistributedDatastoreServlet {

      private DatastoreService datastoreService;
      private ReposeInstanceInfo instanceInfo;

      @Before
      public void setUp() {

         datastoreService = new PowerApiDatastoreService();
         instanceInfo = new ReposeInstanceInfo("repose", "node1");
      }

      @Test
      public void shouldCreateNewServer(){

         DistributedDatastoreJettyServerBuilder builder = new DistributedDatastoreJettyServerBuilder(8888, instanceInfo, "/etc/repose");
         final Server server = builder.newServer(datastoreService, instanceInfo);

         assertTrue(server instanceof Server);

      }
   }
}