package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.net.NetUtilities;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.FilterList;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.NodeList;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class LocalhostFilterListTest {

   public static class WhenUsingLocalhostFilterList {

      private List<Port> getHttpPortList(int port) {
         List<Port> ports = new ArrayList<Port>();
         ports.add(new Port("http", port));
         return ports;
      }

      @Test
      public void shouldInstantiate() {
         SystemModel powerProxy = new SystemModel();
         // TODO Model: HTTPS
         SystemModelInterrogator localhostFilterList = new SystemModelInterrogator(powerProxy, getHttpPortList(8080));

         assertNotNull(localhostFilterList);
      }

      @Test
      public void shouldGetLocalhostFilters() {
         SystemModel mockedPowerProxy = mock(SystemModel.class);
         List<ReposeCluster> domains = createTestHosts();
         when(mockedPowerProxy.getReposeCluster()).thenReturn(domains);

         // TODO Model: HTTPS
         SystemModelInterrogator localhostFilterList = new SystemModelInterrogator(mockedPowerProxy, getHttpPortList(8080));
         ReposeCluster localServiceDomain = localhostFilterList.getLocalServiceDomain();
         
         assertNotNull(localServiceDomain);
         
         List<com.rackspace.papi.model.Filter> filters = localServiceDomain.getFilters().getFilter();

         assertNotNull(filters);
      }

      private List<ReposeCluster> createTestHosts() {
         ReposeCluster domain = new ReposeCluster();
         List<Node> hostList = new ArrayList<Node>();

         domain.setFilters(mock(FilterList.class));

         Node host = new Node();
         host.setHostname(NetUtilities.getLocalHostName());
         host.setHttpPort(8080);
         host.setHttpsPort(0);

         hostList.add(host);

         NodeList nodeList = new NodeList();
         nodeList.getNode().add(host);
         domain.setNodes(nodeList);

         List<ReposeCluster> result = new ArrayList<ReposeCluster>();
         result.add(domain);

         return result;
      }
      // TODO Model: fix test
        /*
       * @Test @Ignore("This test should be reviewed for intent and usage") public void
       * shouldGetLocalhostFiltersWhenNoHostsPresent() { PowerProxy mockedPowerProxy = mock(PowerProxy.class);
       * List<Host> hosts = new ArrayList<Host>(); when(mockedPowerProxy.getHost()).thenReturn(hosts);
       *
       * SystemModelInterrogator localhostFilterList = new SystemModelInterrogator(mockedPowerProxy, 8080);
       * List<com.rackspace.papi.model.Filter> filters = localhostFilterList.getLocalHost().getFilters().getFilter();
       *
       * assertNotNull(filters); }
       *
       */
   }
}
