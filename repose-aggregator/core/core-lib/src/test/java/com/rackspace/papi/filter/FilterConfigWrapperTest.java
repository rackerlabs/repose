package com.rackspace.papi.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class FilterConfigWrapperTest {

    public static final class WhenWrappingFilterConfig {

        private FilterConfig config;
        private FilterConfigWrapper instance;
        private ServletContext context;

        @Before
        public void setUp() {
            config = mock(FilterConfig.class);
            context = mock(ServletContext.class);
            when(config.getServletContext()).thenReturn(context);
            
            instance = new FilterConfigWrapper(config);
        }
        
        @Test
        public void shouldReturnServletContext() {
            ServletContext servletContext = instance.getServletContext();
            assertTrue(servletContext instanceof ServletContextWrapper);
            assertTrue(context == ((ServletContextWrapper)servletContext).getParentContext());
        }
        
        @Test
        public void shouldGetInitParam() {
            final String key1 = "existing";
            final String key2 = "non-existing";
            final String value1 = "value1";
            
            when(config.getInitParameter(eq(key1))).thenReturn(value1);
            
            String actual1 = instance.getInitParameter(key1);
            String actual2 = instance.getInitParameter(key2);
            
            assertEquals(value1, actual1);
            
            assertNull(actual2);
            
        }
        
        @Test
        public void shouldGetParamNames() {
            List<String> list = new ArrayList<String>();
            list.add("key1");
            list.add("key2");
            when(config.getInitParameterNames()).thenReturn(Collections.enumeration(list));
            
            Enumeration<String> keys = instance.getInitParameterNames();
            
            assertNotNull(keys);
            assertTrue(keys.hasMoreElements());
            assertEquals("key1", keys.nextElement());
        }
    }
}
