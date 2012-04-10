package com.rackspace.papi.filter;

import com.rackspace.papi.model.FilterList;
import com.rackspace.papi.model.PowerProxy;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import com.rackspace.papi.commons.util.net.NetUtilities;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.DomainNodeList;
import com.rackspace.papi.model.ServiceDomain;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class LocalhostFilterListTest {
    public static class WhenUsingLocalhostFilterList {
        @Test
        public void shouldInstantiate() {
            PowerProxy powerProxy = new PowerProxy();
            // TODO Model: HTTPS
            SystemModelInterrogator localhostFilterList = new SystemModelInterrogator(powerProxy, 8080, 0);

            assertNotNull(localhostFilterList);
        }

        @Test
        public void shouldGetLocalhostFilters() {
            PowerProxy mockedPowerProxy = mock(PowerProxy.class);
            List<ServiceDomain> domains = createTestHosts();
            when(mockedPowerProxy.getServiceDomain()).thenReturn(domains);

            // TODO Model: HTTPS
            SystemModelInterrogator localhostFilterList = new SystemModelInterrogator(mockedPowerProxy, 8080, 0);
            List<com.rackspace.papi.model.Filter> filters = localhostFilterList.getLocalServiceDomain().getFilters().getFilter();

            assertNotNull(filters);
        }

        private List<ServiceDomain> createTestHosts() {
           ServiceDomain domain = new ServiceDomain();
            List<DomainNode> hostList = new ArrayList<DomainNode>();

            domain.setFilters(mock(FilterList.class));
            
            DomainNode host = new DomainNode();
            host.setHostname(NetUtilities.getLocalHostName());
            host.setHttpPort(8080);
            host.setHttpsPort(0);

            hostList.add(host);
            
            DomainNodeList nodeList = new DomainNodeList();
            nodeList.getNode().add(host);
            domain.setServiceDomainNodes(nodeList);

            List<ServiceDomain> result = new ArrayList<ServiceDomain>();
            result.add(domain);
            
            return result;
        }

        // TODO Model: fix test
        /*
        @Test @Ignore("This test should be reviewed for intent and usage")
        public void shouldGetLocalhostFiltersWhenNoHostsPresent() {
            PowerProxy mockedPowerProxy = mock(PowerProxy.class);
            List<Host> hosts = new ArrayList<Host>();
            when(mockedPowerProxy.getHost()).thenReturn(hosts);

            SystemModelInterrogator localhostFilterList = new SystemModelInterrogator(mockedPowerProxy, 8080);
            List<com.rackspace.papi.model.Filter> filters = localhostFilterList.getLocalHost().getFilters().getFilter();

            assertNotNull(filters);
        }          
        * 
        */
    }
}
