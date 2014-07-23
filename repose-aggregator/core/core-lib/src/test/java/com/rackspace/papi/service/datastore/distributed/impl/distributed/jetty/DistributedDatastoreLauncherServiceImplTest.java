package com.rackspace.papi.service.datastore.distributed.impl.distributed.jetty;

import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.impl.DatastoreServiceImpl;
import com.rackspace.papi.service.datastore.distributed.impl.distributed.servlet.DistributedDatastoreServletContextManager;
import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


@RunWith(Enclosed.class)
public class DistributedDatastoreLauncherServiceImplTest {

   public static class WhenStoppingDistributedDatastoreServlet {

      private DatastoreService datastoreService;
      private ReposeInstanceInfo instanceInfo;

      @Before
      public void setUp() {

         datastoreService = new DatastoreServiceImpl();
         instanceInfo = mock(ReposeInstanceInfo.class);
      }

      @Test
      public void shouldCreateNewServer() throws IllegalAccessException, InstantiationException {

         DistributedDatastoreJettyServerBuilder builder = new DistributedDatastoreJettyServerBuilder(8888, instanceInfo, "/etc/repose", DistributedDatastoreServletContextManager.class.newInstance(), mock(RequestProxyService.class));
         final Server server = builder.newServer(datastoreService, instanceInfo, null, null);

         assertTrue(server instanceof Server);
         server.getStopAtShutdown();


      }
   }
}
