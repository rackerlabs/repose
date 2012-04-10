package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoader;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.commons.util.classloader.ear.EarDescriptor;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import com.rackspace.papi.commons.util.net.NetUtilities;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.DomainNodeList;
import com.rackspace.papi.model.FilterList;
import com.rackspace.papi.model.ServiceDomain;

import javax.servlet.FilterConfig;

import java.util.*;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class PowerFilterChainBuilderTest {
    public static class WhenUsingPowerFilterChainBuilder {
        @Test
        public void shouldInitialize() {
            FilterConfig mockedFilterConfig = mock(FilterConfig.class);
            FilterContextInitializer powerFilterChainBuilder = new FilterContextInitializer(mockedFilterConfig);

            assertNotNull(powerFilterChainBuilder);
        }        
    }

    public static class WhenBuilding {
        private FilterConfig mockedFilterConfig = mock(FilterConfig.class);

        @Test
        public void shouldBuild() throws ClassNotFoundException {
            ClassLoaderManagerService mockedEarClassLoaderContextManager = mock(ClassLoaderManagerService.class);

            EarClassLoaderContext mockedEarClassLoaderContext = mock(EarClassLoaderContext.class);
            EarDescriptor mockedEarDescriptor = mock(EarDescriptor.class);
            Map<String, String> mockedFiltersMap = mock(Map.class);
            EarClassLoader mockedEarClassLoader = mock(EarClassLoader.class);

            when(mockedEarClassLoaderContext.getEarDescriptor()).thenReturn(mockedEarDescriptor);
            when(mockedEarDescriptor.getRegisteredFilters()).thenReturn(mockedFiltersMap);
            when(mockedEarClassLoaderContext.getClassLoader()).thenReturn(mockedEarClassLoader);
            when(mockedFiltersMap.get(any(String.class))).thenReturn("FilterClassName");
            when(mockedEarClassLoader.loadClass(any(String.class))).thenReturn((Class) FakeFilterClass.class);

            Collection<EarClassLoaderContext> loadedApplications = new LinkedList<EarClassLoaderContext>();
            loadedApplications.add(mockedEarClassLoaderContext);

            when(mockedEarClassLoaderContextManager.getLoadedApplications()).thenReturn(loadedApplications);

            Filter mockedFilter = mock(Filter.class);
            when(mockedFilter.getName()).thenReturn("filterName");

            FilterContextInitializer powerFilterChainBuilder = new FilterContextInitializer(mockedFilterConfig);

            PowerProxy mockedPowerProxy = mock(PowerProxy.class);
            List<ServiceDomain> hosts = createTestHosts();
            when(mockedPowerProxy.getServiceDomain()).thenReturn(hosts);

            List<FilterContext> powerFilterChain = powerFilterChainBuilder.buildFilterContexts(mockedEarClassLoaderContextManager, mockedPowerProxy, 8080);

            assertNotNull(powerFilterChain);
        }

        @Test
        public void shouldReturnEmptyList() throws ClassNotFoundException {
            ClassLoaderManagerService mockedEarClassLoaderContextManager = mock(ClassLoaderManagerService.class);

            EarClassLoaderContext mockedEarClassLoaderContext = mock(EarClassLoaderContext.class);
            EarDescriptor mockedEarDescriptor = mock(EarDescriptor.class);
            Map<String, String> mockedFiltersMap = mock(Map.class);
            EarClassLoader mockedEarClassLoader = mock(EarClassLoader.class);

            when(mockedEarClassLoaderContext.getEarDescriptor()).thenReturn(mockedEarDescriptor);
            when(mockedEarDescriptor.getRegisteredFilters()).thenReturn(mockedFiltersMap);
            when(mockedEarClassLoaderContext.getClassLoader()).thenReturn(mockedEarClassLoader);
            when(mockedFiltersMap.get(any(String.class))).thenReturn("FilterClassName");
            when(mockedEarClassLoader.loadClass(any(String.class))).thenReturn(null);

            Collection<EarClassLoaderContext> loadedApplications = new LinkedList<EarClassLoaderContext>();
            loadedApplications.add(mockedEarClassLoaderContext);

            when(mockedEarClassLoaderContextManager.getLoadedApplications()).thenReturn(loadedApplications);

            Filter mockedFilter = mock(Filter.class);
            when(mockedFilter.getName()).thenReturn("filterName");

            FilterContextInitializer powerFilterChainBuilder = new FilterContextInitializer(mockedFilterConfig);

            PowerProxy mockedPowerProxy = mock(PowerProxy.class);
            List<ServiceDomain> hosts = createTestHosts();
            when(mockedPowerProxy.getServiceDomain()).thenReturn(hosts);

            List<FilterContext> powerFilterChain = powerFilterChainBuilder.buildFilterContexts(mockedEarClassLoaderContextManager, mockedPowerProxy, 8080);

            assertEquals(0, powerFilterChain.size());
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

    }
}
