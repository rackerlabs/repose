package com.rackspace.papi.filter;

import com.rackspace.papi.http.proxy.HttpRequestDispatcher;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.servlet.InitParameter;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.springframework.context.ApplicationContext;

@RunWith(Enclosed.class)
public class ServletContextWrapperTest {

   public static class When {
      private ServletContextWrapper instance;
      private ServletContext contextMock;
      private String contextPath = "/someContext";
      private ApplicationContext appContext;
      
      @Before
      public void setUp() {
         contextMock = mock(ServletContext.class);
         appContext = mock(ApplicationContext.class);
         
         when(contextMock.getContextPath()).thenReturn(contextPath);
         when(contextMock.getAttribute(eq(ServletContextHelper.SPRING_APPLICATION_CONTEXT_ATTRIBUTE_NAME))).thenReturn(appContext);
         when(contextMock.getAttribute(InitParameter.READ_TIMEOUT.getParameterName())).thenReturn(new Integer(10));
         instance = (ServletContextWrapper) ServletContextWrapper.getContext(contextMock);
      }
      
      @Test
      public void shouldReturnNonNullWrapper() {
         assertNotNull(instance);
      }
      
      @Test
      public void shouldReturnContextPath() {
         assertEquals(contextPath, instance.getContextPath());
      }
      
      @Test
      public void shouldCallUnderlyingGetContext() {
         final String subcontext = "/somepath";
         instance.getContext(subcontext);
         
         verify(contextMock).getContext(subcontext);
      }
      
      @Test
      public void shouldReturnWrappedContextForFullURL() {
         final String target = "somehost:8080";
         final String subcontext = "http://" + target + "/somepath";
         ServletContext context = instance.getContext(subcontext);
         assertTrue(context instanceof ServletContextWrapper);
         ServletContextWrapper wrapped = (ServletContextWrapper) context;
         assertEquals(instance, wrapped.getParentContext());
         assertEquals(subcontext, wrapped.getTargetContext());
         assertEquals(target, wrapped.getTarget());
      }
      
      @Test
      public void shouldCallUnderlyingGetDispatcher() {
         final String subcontext = "/somepath";
         instance.getRequestDispatcher(subcontext);
         
         verify(contextMock).getRequestDispatcher(subcontext);
         
      }
      
      @Test
      public void shouldReturnRemoteDispatcherForFullUrl() {
         final String target = "somehost:8080";
         final String subcontext = "http://" + target + "/somepath";
         ServletContext context = instance.getContext(subcontext);
         assertTrue(context instanceof ServletContextWrapper);
         ServletContextWrapper wrapped = (ServletContextWrapper) context;

         RequestDispatcher requestDispatcher = wrapped.getRequestDispatcher(subcontext);
         
         assertNotNull(requestDispatcher);
         assertTrue(requestDispatcher instanceof HttpRequestDispatcher);
      }
      
   }
}
