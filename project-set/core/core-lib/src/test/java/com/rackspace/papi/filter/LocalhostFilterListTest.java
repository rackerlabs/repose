package com.rackspace.papi.filter;

import com.rackspace.papi.filter.LocalhostFilterList;
import com.rackspace.papi.model.FilterList;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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
            LocalhostFilterList localhostFilterList = new LocalhostFilterList(powerProxy);

            assertNotNull(localhostFilterList);
        }

        @Test
        public void shouldGetLocalhostFilters() {
            PowerProxy mockedPowerProxy = mock(PowerProxy.class);
            List<Host> hosts = createTestHosts();
            when(mockedPowerProxy.getHost()).thenReturn(hosts);

            LocalhostFilterList localhostFilterList = new LocalhostFilterList(mockedPowerProxy);
            List<com.rackspace.papi.model.Filter> filters = localhostFilterList.getFilters();

            assertNotNull(filters);
        }

        private List<Host> createTestHosts() {
            List<Host> hostList = new ArrayList<Host>();

            Host host = new Host();
            host.setHostname(LocalhostFilterList.getLocalHostName());
            host.setFilters(mock(FilterList.class));

            hostList.add(host);

            return hostList;
        }

        @Test
        public void shouldGetLocalhostFiltersWhenNoHostsPresent() {
            PowerProxy mockedPowerProxy = mock(PowerProxy.class);
            List<Host> hosts = new ArrayList<Host>();
            when(mockedPowerProxy.getHost()).thenReturn(hosts);

            LocalhostFilterList localhostFilterList = new LocalhostFilterList(mockedPowerProxy);
            List<com.rackspace.papi.model.Filter> filters = localhostFilterList.getFilters();

            assertNotNull(filters);
        }                               
    }
}
