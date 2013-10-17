/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore.impl.distributed.jetty;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.impl.PowerApiDatastoreService;
import com.rackspace.papi.service.datastore.impl.distributed.servlet.DistributedDatastoreServletContextManager;
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
         
         datastoreService = new PowerApiDatastoreService();
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