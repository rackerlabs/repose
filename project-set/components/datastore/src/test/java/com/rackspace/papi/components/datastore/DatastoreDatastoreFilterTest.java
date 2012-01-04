package com.rackspace.papi.components.datastore;

import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ConfigurationServiceContext;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.context.DatastoreServiceContext;
import javax.naming.Context;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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
         final ConfigurationService configurationService = mock(ConfigurationService.class);
         final ServiceContext<ConfigurationService> configurationServiceContext = mock(ServiceContext.class);
         final ServiceContext<DatastoreService> datastoreServiceContext = mock(ServiceContext.class);
         final DatastoreManager localManager = mock(DatastoreManager.class);

         when(mockFilterConfig.getServletContext()).thenReturn(servletContext);
         when(servletContext.getAttribute(ServletContextHelper.SERVLET_CONTEXT_ATTRIBUTE_NAME)).thenReturn(context);

         when(context.lookup(ConfigurationServiceContext.SERVICE_NAME)).thenReturn(configurationServiceContext);
         when(context.lookup(DatastoreServiceContext.SERVICE_NAME)).thenReturn(datastoreServiceContext);

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

         verify(datastoreService, times(1)).registerDatastoreManager(eq(DATASTORE_MANAGER_NAME), any(DatastoreManager.class));
         verify(datastoreService, times(1)).unregisterDatastoreManager(DATASTORE_MANAGER_NAME);
      }
   }
}
