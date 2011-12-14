package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoader;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.commons.util.classloader.ear.EarDescriptor;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.classloader.ApplicationClassLoaderManager;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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
            ApplicationClassLoaderManager mockedEarClassLoaderContextManager = mock(ApplicationClassLoaderManager.class);

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
            List<Host> hosts = createTestHosts();
            when(mockedPowerProxy.getHost()).thenReturn(hosts);

            List<FilterContext> powerFilterChain = powerFilterChainBuilder.buildFilterContexts(mockedEarClassLoaderContextManager, mockedPowerProxy);

            assertNotNull(powerFilterChain);
        }

        @Test
        public void shouldReturnEmptyList() throws ClassNotFoundException {
            ApplicationClassLoaderManager mockedEarClassLoaderContextManager = mock(ApplicationClassLoaderManager.class);

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
            List<Host> hosts = createTestHosts();
            when(mockedPowerProxy.getHost()).thenReturn(hosts);

            List<FilterContext> powerFilterChain = powerFilterChainBuilder.buildFilterContexts(mockedEarClassLoaderContextManager, mockedPowerProxy);

            assertEquals(0, powerFilterChain.size());
        }

        private List<Host> createTestHosts() {
            List<Host> hostList = new ArrayList<Host>();

            Filter mockedFilter = mock(Filter.class);
            FakeFilterListClass filterListClass = new FakeFilterListClass();
            filterListClass.addFilter(mockedFilter);

            Host host = new Host();
            host.setHostname(SystemModelInterrogator.getLocalHostName());
            host.setFilters(filterListClass);

            hostList.add(host);

            return hostList;
        }
    }
}
