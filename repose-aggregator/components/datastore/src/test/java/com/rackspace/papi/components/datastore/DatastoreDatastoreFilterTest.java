package com.rackspace.papi.components.datastore;

import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContextName;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.impl.ConfigurationServiceContext;
import com.rackspace.papi.service.context.impl.DatastoreServiceContext;
import com.rackspace.papi.service.context.impl.RequestProxyServiceContext;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;

import javax.naming.Context;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class DatastoreDatastoreFilterTest {

   public static final String DATASTORE_MANAGER_NAME = "name";

   @Ignore
   public static class TestParent {

      protected FilterConfig mockFilterConfig;
      protected DatastoreService datastoreService;
      protected DistributedDatastoreFilter filter;

      @Before
      public final void beforeAll() throws Exception {
         datastoreService = mock(DatastoreService.class);
         mockFilterConfig = mock(FilterConfig.class);

         final ServletContext servletContext = mock(ServletContext.class);
         final Context context = mock(Context.class);
         final ApplicationContext appContext = mock(ApplicationContext.class);
         final ConfigurationService configurationService = mock(ConfigurationService.class);
         final ConfigurationServiceContext configurationServiceContext = mock(ConfigurationServiceContext.class);
         final DatastoreServiceContext datastoreServiceContext = mock(DatastoreServiceContext.class);
         final DatastoreManager localManager = mock(DatastoreManager.class);
         final RequestProxyServiceContext proxyService = mock(RequestProxyServiceContext.class);

         ServletContextHelper instance = ServletContextHelper.configureInstance(servletContext, appContext);
         
         when(mockFilterConfig.getServletContext()).thenReturn(servletContext);
         when(servletContext.getAttribute(ServletContextHelper.SERVLET_CONTEXT_ATTRIBUTE_NAME)).thenReturn(context);
         when(servletContext.getAttribute(ServletContextHelper.SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME)).thenReturn(appContext);
         when(servletContext.getAttribute(ServletContextHelper.SERVLET_CONTEXT_HELPER)).thenReturn(instance);

         when(appContext.getBean(eq(ServiceContextName.CONFIGURATION_SERVICE_CONTEXT.getServiceContextName()))).thenReturn(configurationServiceContext);
         when(appContext.getBean(eq(ServiceContextName.DATASTORE_SERVICE_CONTEXT.getServiceContextName()))).thenReturn(datastoreServiceContext);
         when(appContext.getBean(eq(ServiceContextName.REQUEST_PROXY_SERVICE_CONTEXT.getServiceContextName()))).thenReturn(proxyService);
         when(appContext.getBean(anyString(), eq(ServicePorts.class))).thenReturn(new ServicePorts());

         when(configurationServiceContext.getService()).thenReturn(configurationService);
         when(datastoreServiceContext.getService()).thenReturn(datastoreService);

         when(datastoreService.defaultDatastore()).thenReturn(localManager);
         when(localManager.isAvailable()).thenReturn(true);

         filter = new DistributedDatastoreFilter(DATASTORE_MANAGER_NAME);
      }
   }

   public static class WhenRegisteringDatastore extends TestParent {

      @Test
      public void shouldRegisterDatastore() throws Exception {
         filter.init(mockFilterConfig);

         verify(datastoreService).registerDatastoreManager(eq(DATASTORE_MANAGER_NAME), any(DatastoreManager.class));
      }

      @Test
      public void shouldUnRegisterDatastore() throws Exception {
         filter.init(mockFilterConfig);
         filter.destroy();

         verify(datastoreService).registerDatastoreManager(eq(DATASTORE_MANAGER_NAME), any(DatastoreManager.class));
         verify(datastoreService).unregisterDatastoreManager(DATASTORE_MANAGER_NAME);
      }
   }
}
