package com.rackspace.papi.commons.util.servlet.filter;

import com.rackspace.papi.commons.util.servlet.InitParameter;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.*;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: May 20, 2011
 * Time: 2:52:26 PM
 */
@RunWith(Enclosed.class)
public class ApplicationContextAwareFilterTest {
    public static class WhenInitializing {
        private FilterConfig filterConfig;
        private ServletContext servletContext;

        @Before
        public void setup() {
            filterConfig = mock(FilterConfig.class);
            servletContext = mock(ServletContext.class);

            when(filterConfig.getServletContext()).thenReturn(servletContext);
        }

        @Test
        public void shouldInitializeAppContext() throws ServletException {
            when(filterConfig.getInitParameter(InitParameter.APP_CONTEXT_ADAPTER_CLASS.getParameterName()))
                    .thenReturn(SampleApplicationContextAdapter.class.getCanonicalName());
            ApplicationContextAwareFilter filter = new SampleFilter();

            filter.init(filterConfig);

            assertNotNull(filter.getAppContext());
        }

        @Test
        public void shouldNotInitializeAppContextIfInitParameterIsEmpty() throws ServletException {
            when(filterConfig.getInitParameter(InitParameter.APP_CONTEXT_ADAPTER_CLASS.getParameterName()))
                    .thenReturn("");
            ApplicationContextAwareFilter filter = new SampleFilter();

            filter.init(filterConfig);

            assertNull(filter.getAppContext());
        }

        @Test
        public void shouldNotInitializeAppContextIfInitParameterIsNull() throws ServletException {
            when(filterConfig.getInitParameter(InitParameter.APP_CONTEXT_ADAPTER_CLASS.getParameterName()))
                    .thenReturn(null);
            ApplicationContextAwareFilter filter = new SampleFilter();

            filter.init(filterConfig);

            assertNull(filter.getAppContext());
        }
    }

    public static class WhenInitializingAndAdapterClassIsNotFound {
        private FilterConfig filterConfig;

        @Before
        public void setup() {
            filterConfig = mock(FilterConfig.class);

            when(filterConfig.getInitParameter(InitParameter.APP_CONTEXT_ADAPTER_CLASS.getParameterName()))
                    .thenReturn(null);
        }

        @Test
        public void shouldHaveNullAppContext() throws ServletException {
            ApplicationContextAwareFilter filter = new SampleFilter();

            filter.init(filterConfig);

            assertNull(filter.getAppContext());
        }
    }

    public static class WhenInitializingAndAdapterClassIsNotSpecified {
        private FilterConfig filterConfig;

        @Before
        public void setup() {
            filterConfig = mock(FilterConfig.class);

            when(filterConfig.getInitParameter(InitParameter.APP_CONTEXT_ADAPTER_CLASS.getParameterName()))
                    .thenReturn("");
        }

        @Test
        public void shouldHaveNullAppContext() throws ServletException {
            ApplicationContextAwareFilter filter = new SampleFilter();

            filter.init(filterConfig);

            assertNull(filter.getAppContext());
        }
    }

    public static class WhenInstantiating {
        @Test
        public void shouldHaveNullAppContextByDefault() {
            ApplicationContextAwareFilter filter = new SampleFilter();

            assertNull(filter.getAppContext());
        }
    }

    private static class SampleFilter extends ApplicationContextAwareFilter {

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void destroy() {
            throw new UnsupportedOperationException();
        }
    }
}
