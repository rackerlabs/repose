package com.rackspace.papi.components.logging;

import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.impl.ConfigurationServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.context.jndi.JndiContextAdapterProvider;
import java.io.IOException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class HttpLoggingFilterTest {

   @Ignore
   public static class BaseTest {

      protected HttpLoggingFilter filter;
      protected FilterConfig filterConfig;
      protected ServletContext servletContext;
      protected Context context;
      protected ServiceContext<ConfigurationService> serviceContext;
      protected ConfigurationService configService;
      protected FilterChain filterChain;

      @Before
      public void setup() throws NamingException {
         filter = new HttpLoggingFilter();
         filterConfig = mock(FilterConfig.class);
         servletContext = mock(ServletContext.class);
         context = mock(Context.class);
         serviceContext = mock(ServiceContext.class);
         configService = mock(ConfigurationService.class);
         filterChain = mock(FilterChain.class);
         ServletContextHelper.configureInstance(new JndiContextAdapterProvider(), servletContext, context);

         when(filterConfig.getServletContext()).thenReturn(servletContext);
         when(servletContext.getAttribute(ServletContextHelper.SERVLET_CONTEXT_ATTRIBUTE_NAME)).thenReturn(context);
         when(context.lookup(ConfigurationServiceContext.SERVICE_NAME)).thenReturn(serviceContext);
         when(serviceContext.getService()).thenReturn(configService);

      }
   }

   public static class WhenInitializing extends BaseTest {

      @Test
      public void shouldSubscribeToConfigFile() throws ServletException {
         filter.init(filterConfig);
         verify(configService).subscribeTo(eq("http-logging.cfg.xml"), any(HttpLoggingHandlerFactory.class), any(Class.class));
      }
   }

   public static class WhenDestroying extends BaseTest {

      @Test
      public void shouldUnsubscribeFromConfigFile() throws ServletException {
         filter.init(filterConfig);
         filter.destroy();
         verify(configService).unsubscribeFrom(eq("http-logging.cfg.xml"), any(HttpLoggingHandlerFactory.class));
      }
   }

   public static class WhenFiltering extends BaseTest {

      private HttpServletRequest request;
      private HttpServletResponse response;
      private Hashtable<String, String> headers;

      @Before
      public void setupFiltering() {
         request = mock(HttpServletRequest.class);
         response = mock(HttpServletResponse.class);
         headers = new Hashtable<String, String>();

         when(request.getHeaderNames()).thenReturn(headers.keys());
      }

      @Test
      public void shouldCallFilterChainDoFilter() throws ServletException, IOException {
         filter.init(filterConfig);
         filter.doFilter(request, response, filterChain);
         filter.destroy();
         verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
      }
   }
}
