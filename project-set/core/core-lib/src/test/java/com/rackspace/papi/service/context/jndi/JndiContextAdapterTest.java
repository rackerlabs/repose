package com.rackspace.papi.service.context.jndi;

import com.rackspace.papi.service.ServiceUnavailableException;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.impl.ClassLoaderServiceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.NamingException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * TODO: Finish testing the rest of the service names and their associated methods on the context adapter
 * 
 * @author zinic
 */
@RunWith(Enclosed.class)
public class JndiContextAdapterTest {

   public static class WhenLookingUpServices {

      public static final String EXCEPTION_CASE = "THROW_EXCEPTION >:E";
      
      protected ClassLoaderManagerService classLoaderManagerService;
      protected JndiContextAdapter adapter;
      protected Context namingContext;
      
      @Before
      public void standUp() throws Exception {
         namingContext = mock(Context.class);
         adapter = new JndiContextAdapter(namingContext);

         final ServiceContext<ClassLoaderManagerService> configurationServiceContext = mock(ServiceContext.class);
         classLoaderManagerService = mock(ClassLoaderManagerService.class);

         when(namingContext.lookup(ClassLoaderServiceContext.SERVICE_NAME)).thenReturn(configurationServiceContext);
         when(namingContext.lookup(EXCEPTION_CASE)).thenThrow(new NamingException());
         
         when(configurationServiceContext.getService()).thenReturn(classLoaderManagerService);
      }

      @Test
      public void shouldLookupServiceByName() throws Exception {
         final ClassLoaderManagerService actual = adapter.classLoader();
         
         assertEquals("The service returned must match the expected reference", classLoaderManagerService, actual);
         verify(namingContext, times(1)).lookup(ClassLoaderServiceContext.SERVICE_NAME);
      }

      @Test(expected=ServiceUnavailableException.class)
      public void shouldCommunicateServiceFailureOnNamingExceptions() throws Exception {
         new JndiContextAdapter(namingContext).lookup(EXCEPTION_CASE);
      }
   }
}
