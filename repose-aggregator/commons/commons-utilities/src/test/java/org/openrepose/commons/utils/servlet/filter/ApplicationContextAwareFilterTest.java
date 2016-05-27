/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.servlet.filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.servlet.InitParameter;

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
