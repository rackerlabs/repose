/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContextName;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.container.ContainerConfigurationService;
import com.rackspace.papi.service.context.impl.ConfigurationServiceContext;
import com.rackspace.papi.service.context.impl.ContainerServiceContext;
import com.rackspace.papi.service.context.impl.DatastoreServiceContext;
import com.rackspace.papi.service.context.impl.RequestProxyServiceContext;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import javax.naming.Context;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import static org.mockito.Mockito.*;

/**
 *
 * @author kush5342
 */
@RunWith(Enclosed.class)
public class ReplicatedDatastoreFilterTest {
    
    private static final String CACHE_MANAGER_NAME = "ReplicatedDatastoreCacheManager";
    //private static ReplicatedDatastoreFilterHandlerFactory handlerFactory;
    //private static CacheManager ehCacheManager;

   @Ignore
   public static class TestParent {

      protected FilterConfig mockFilterConfig;
      protected DatastoreService datastoreService;
      protected ReplicatedDatastoreFilter filter;
      protected  ConfigurationService configurationService;

      @Before
      public final void beforeAll() throws Exception {
      
         mockFilterConfig = mock(FilterConfig.class);

         final ServletContext servletContext = mock(ServletContext.class);
         final Context context = mock(Context.class);
         final ApplicationContext appContext = mock(ApplicationContext.class);
         
         final ConfigurationServiceContext configurationServiceContext = mock(ConfigurationServiceContext.class);
         final ContainerServiceContext containerServiceContext=mock(ContainerServiceContext.class);
         final ContainerConfigurationService containerConfigurationService=mock(ContainerConfigurationService.class);
         final DatastoreServiceContext datastoreServiceContext = mock(DatastoreServiceContext.class);
         final DatastoreManager localManager = mock(DatastoreManager.class);
         final RequestProxyServiceContext proxyService = mock(RequestProxyServiceContext.class);
         final ReposeInstanceInfo reposeInstanceInfo = mock(ReposeInstanceInfo.class);
          
       
         
        configurationService = mock(ConfigurationService.class);
         ServletContextHelper instance = ServletContextHelper.configureInstance(servletContext, appContext);
         
         when(mockFilterConfig.getServletContext()).thenReturn(servletContext);
         when(servletContext.getAttribute(ServletContextHelper.SERVLET_CONTEXT_ATTRIBUTE_NAME)).thenReturn(context);
         when(servletContext.getAttribute(ServletContextHelper.SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME)).thenReturn(appContext);
         when(servletContext.getAttribute(ServletContextHelper.SERVLET_CONTEXT_HELPER)).thenReturn(instance);

         when(appContext.getBean(eq(ServiceContextName.CONFIGURATION_SERVICE_CONTEXT.getServiceContextName()))).thenReturn(configurationServiceContext);
         when(appContext.getBean(eq(ServiceContextName.DATASTORE_SERVICE_CONTEXT.getServiceContextName()))).thenReturn(datastoreServiceContext);
         when(appContext.getBean(eq(ServiceContextName.REQUEST_PROXY_SERVICE_CONTEXT.getServiceContextName()))).thenReturn(proxyService);
         when(appContext.getBean(anyString(), eq(ServicePorts.class))).thenReturn(new ServicePorts());
         when(appContext.getBean(eq("reposeInstanceInfo"), eq(ReposeInstanceInfo.class))).thenReturn(reposeInstanceInfo);
         when(appContext.getBean(eq(ServiceContextName.CONTAINER_SERVICE_CONTEXT.getServiceContextName()))).thenReturn(containerServiceContext);
         when(containerServiceContext.getService()).thenReturn(containerConfigurationService);
         
         when(configurationServiceContext.getService()).thenReturn(configurationService);
       
       filter = new ReplicatedDatastoreFilter();
      }
   }

   public static class WhenRegisteringDatastore extends TestParent {

      @Test
      public void shouldRegisterDatastore() throws Exception {
         filter.init(mockFilterConfig);
         verify(configurationService,times(2)).subscribeTo(anyString(),anyString(),any(UpdateListener.class),any(Class.class));
      }

      @Test
      public void shouldUnRegisterDatastore() throws Exception {
         filter.init(mockFilterConfig);
         filter.destroy();
         verify(configurationService,times(3)).unsubscribeFrom(anyString(),any(UpdateListener.class)); 
         
        
      }
   }
}