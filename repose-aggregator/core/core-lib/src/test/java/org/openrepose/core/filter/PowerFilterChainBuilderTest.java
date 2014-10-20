package org.openrepose.core.filter;

import com.oracle.javaee6.FilterType;
import com.oracle.javaee6.FullyQualifiedClassType;
import org.openrepose.commons.utils.classloader.ear.EarClassLoader;
import org.openrepose.commons.utils.classloader.ear.EarClassLoaderContext;
import org.openrepose.commons.utils.classloader.ear.EarDescriptor;
import org.openrepose.core.domain.Port;
import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.core.domain.ServicePorts;
import org.openrepose.core.systemmodel.*;
import org.openrepose.core.services.classloader.ClassLoaderManagerService;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;

import javax.servlet.FilterConfig;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class PowerFilterChainBuilderTest {

   public static class WhenUsingPowerFilterChainBuilder {

      @Test
      public void shouldInitialize() {
         FilterConfig mockedFilterConfig = mock(FilterConfig.class);
         ApplicationContext context = mock(ApplicationContext.class);
         ReposeInstanceInfo info = new ReposeInstanceInfo("cluster", "node");
         when(context.getBean("reposeInstanceInfo")).thenReturn(info);
         
         FilterContextInitializer powerFilterChainBuilder = new FilterContextInitializer(mockedFilterConfig);

         assertNotNull(powerFilterChainBuilder);
      }
   }

   public static class WhenBuilding {

      private FilterConfig mockedFilterConfig = mock(FilterConfig.class);
      /*
       * EarClassLoaderContext mockedEarClassLoaderContext = mock(EarClassLoaderContext.class); EarDescriptor
       * mockedEarDescriptor = mock(EarDescriptor.class); Map<String, FilterType> mockedFiltersMap = mock(Map.class);
       * EarClassLoader mockedEarClassLoader = mock(EarClassLoader.class); FilterType mockedFilterType =
       * mock(FilterType.class); FullyQualifiedClassType mockedClassType = mock(FullyQualifiedClassType.class);
       *
       * when(mockedEarClassLoaderContext.getEarDescriptor()).thenReturn(mockedEarDescriptor);
       * when(mockedEarDescriptor.getRegisteredFilters()).thenReturn(mockedFiltersMap);
       * when(mockedEarClassLoaderContext.getClassLoader()).thenReturn(mockedEarClassLoader);
       * when(mockedFilterType.getFilterClass()).thenReturn(mockedClassType);
       * when(mockedClassType.getValue()).thenReturn("FilterClassName");
       * when(mockedEarClassLoader.loadClass(any(String.class))).thenReturn((Class) FakeFilterClass.class);
       */

      private ServicePorts getHttpPortList(int port) {
         ServicePorts ports = new ServicePorts();
         ports.add(new Port("http", port));
         return ports;
      }

      @Test
      public void shouldBuild() throws ClassNotFoundException {
         ClassLoaderManagerService mockedEarClassLoaderContextManager = mock(ClassLoaderManagerService.class);

         EarClassLoaderContext mockedEarClassLoaderContext = mock(EarClassLoaderContext.class);
         EarDescriptor mockedEarDescriptor = mock(EarDescriptor.class);
         Map<String, FilterType> mockedFiltersMap = mock(Map.class);
         EarClassLoader mockedEarClassLoader = mock(EarClassLoader.class);
         FilterType mockedFilterType = mock(FilterType.class);
         FullyQualifiedClassType mockedClassType = mock(FullyQualifiedClassType.class);

         when(mockedEarClassLoaderContext.getEarDescriptor()).thenReturn(mockedEarDescriptor);
         when(mockedEarDescriptor.getRegisteredFilters()).thenReturn(mockedFiltersMap);
         when(mockedEarClassLoaderContext.getClassLoader()).thenReturn(mockedEarClassLoader);
         when(mockedFilterType.getFilterClass()).thenReturn(mockedClassType);
         when(mockedClassType.getValue()).thenReturn("FilterClassName");
         when(mockedEarClassLoader.loadClass(any(String.class))).thenReturn((Class) FakeFilterClass.class);

         Collection<EarClassLoaderContext> loadedApplications = new LinkedList<EarClassLoaderContext>();
         loadedApplications.add(mockedEarClassLoaderContext);

         when(mockedEarClassLoaderContextManager.getLoadedApplications()).thenReturn(loadedApplications);

         Filter mockedFilter = mock(Filter.class);
         when(mockedFilter.getName()).thenReturn("filterName");

         ApplicationContext context = mock(ApplicationContext.class);
         ReposeInstanceInfo info = new ReposeInstanceInfo("cluster", "node");
         when(context.getBean("reposeInstanceInfo")).thenReturn(info);
         FilterContextInitializer powerFilterChainBuilder = new FilterContextInitializer(mockedFilterConfig);

         SystemModel mockedPowerProxy = mock(SystemModel.class);
         List<ReposeCluster> hosts = createTestHosts();
         when(mockedPowerProxy.getReposeCluster()).thenReturn(hosts);
         
         SystemModelInterrogator interrogator = new SystemModelInterrogator(getHttpPortList(8080));
         Node localHost = interrogator.getLocalNode(mockedPowerProxy).get();
         ReposeCluster serviceDomain = interrogator.getLocalCluster(mockedPowerProxy).get();

         List<FilterContext> powerFilterChain = powerFilterChainBuilder.buildFilterContexts(
                 mockedEarClassLoaderContextManager,
                 serviceDomain,
                 localHost);

         assertNotNull(powerFilterChain);
      }

      @Test
      public void shouldReturnEmptyList() throws ClassNotFoundException {
         ClassLoaderManagerService mockedEarClassLoaderContextManager = mock(ClassLoaderManagerService.class);

         EarClassLoaderContext mockedEarClassLoaderContext = mock(EarClassLoaderContext.class);
         EarDescriptor mockedEarDescriptor = mock(EarDescriptor.class);
         Map<String, FilterType> mockedFiltersMap = mock(Map.class);
         EarClassLoader mockedEarClassLoader = mock(EarClassLoader.class);
         FilterType mockedFilterType = mock(FilterType.class);
         FullyQualifiedClassType mockedClassType = mock(FullyQualifiedClassType.class);

         when(mockedEarClassLoaderContext.getEarDescriptor()).thenReturn(mockedEarDescriptor);
         when(mockedEarDescriptor.getRegisteredFilters()).thenReturn(mockedFiltersMap);
         when(mockedEarClassLoaderContext.getClassLoader()).thenReturn(mockedEarClassLoader);
         when(mockedFilterType.getFilterClass()).thenReturn(mockedClassType);
         when(mockedClassType.getValue()).thenReturn("FilterClassName");
         when(mockedFiltersMap.get(any(String.class))).thenReturn(mockedFilterType);
         when(mockedEarClassLoader.loadClass(any(String.class))).thenReturn(null);

         Collection<EarClassLoaderContext> loadedApplications = new LinkedList<EarClassLoaderContext>();
         loadedApplications.add(mockedEarClassLoaderContext);

         when(mockedEarClassLoaderContextManager.getLoadedApplications()).thenReturn(loadedApplications);

         Filter mockedFilter = mock(Filter.class);
         when(mockedFilter.getName()).thenReturn("filterName");

         ApplicationContext context = mock(ApplicationContext.class);
         ReposeInstanceInfo info = new ReposeInstanceInfo("cluster", "node");
         when(context.getBean("reposeInstanceInfo")).thenReturn(info);
         FilterContextInitializer powerFilterChainBuilder = new FilterContextInitializer(mockedFilterConfig);

         SystemModel mockedPowerProxy = mock(SystemModel.class);
         List<ReposeCluster> hosts = createTestHosts();
         when(mockedPowerProxy.getReposeCluster()).thenReturn(hosts);
         SystemModelInterrogator interrogator = new SystemModelInterrogator(getHttpPortList(8080));
         Node localHost = interrogator.getLocalNode(mockedPowerProxy).get();
         ReposeCluster serviceDomain = interrogator.getLocalCluster(mockedPowerProxy).get();

         List<FilterContext> powerFilterChain = powerFilterChainBuilder
                 .buildFilterContexts(mockedEarClassLoaderContextManager, serviceDomain, localHost);

         assertEquals(0, powerFilterChain.size());
      }

      private List<ReposeCluster> createTestHosts() {
         ReposeCluster domain = new ReposeCluster();
         List<Node> hostList = new ArrayList<Node>();

         domain.setFilters(mock(FilterList.class));

         Node host = new Node();
         host.setHostname("localhost");
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
   }
}
