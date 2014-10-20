package org.openrepose.core.filter;

import com.oracle.javaee6.FilterType;
import com.oracle.javaee6.FullyQualifiedClassType;
import org.junit.Ignore;
import org.openrepose.commons.utils.classloader.ear.EarClassLoader;
import org.openrepose.commons.utils.classloader.ear.EarClassLoaderContext;
import org.openrepose.commons.utils.classloader.ear.EarDescriptor;
import org.openrepose.core.systemmodel.Filter;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;

import javax.servlet.FilterConfig;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
@RunWith(Enclosed.class)
public class FilterContextManagerTest {

    public static class WhenLoadingFilterContext {
        private FilterConfig mockedFilterConfig;
        private FilterContextManager contextManager;
        private Filter filter;

        @Before
        public void setup() {
            filter = new Filter();
            filter.setName("FilterName");
            filter.setUriRegex(".*");
            contextManager = null;
            mockedFilterConfig = mock(FilterConfig.class);
        }

        private EarClassLoaderContext getMockedEarClassLoader(String filterClassName, Boolean loadThrowsException) throws ClassNotFoundException {
            EarClassLoaderContext mockedEarClassLoaderContext = mock(EarClassLoaderContext.class);
            EarDescriptor mockedEarDescriptor = mock(EarDescriptor.class);
            Map<String, FilterType> mockedFiltersMap = mock(Map.class);
            EarClassLoader mockedEarClassLoader = mock(EarClassLoader.class);
            FilterType mockedFilterType = mock(FilterType.class);
            FullyQualifiedClassType mockedClassType = mock(FullyQualifiedClassType.class);

            when(mockedEarClassLoaderContext.getEarDescriptor()).thenReturn(mockedEarDescriptor);
            when(mockedEarDescriptor.getRegisteredFilters()).thenReturn(mockedFiltersMap);
            when(mockedEarClassLoaderContext.getClassLoader()).thenReturn(mockedEarClassLoader);
            when(mockedFiltersMap.get(any(String.class))).thenReturn(mockedFilterType);
            when(mockedFilterType.getFilterClass()).thenReturn(mockedClassType);
            when(mockedClassType.getValue()).thenReturn(filterClassName);

            if (loadThrowsException) {
                when(mockedEarClassLoader.loadClass(any(String.class))).thenThrow(new ClassNotFoundException());
            } else {
                when(mockedEarClassLoader.loadClass(any(String.class))).thenReturn((Class) FakeFilterClass.class);
            }

            return mockedEarClassLoaderContext;
        }

        @Test
        public void shouldLoadFilterContext() throws ClassNotFoundException, FilterInitializationException {
            EarClassLoaderContext mockedEarClassLoaderContext = getMockedEarClassLoader("FilterClassName", false);
            Collection<EarClassLoaderContext> loadedApplications = new LinkedList<EarClassLoaderContext>();
            loadedApplications.add(mockedEarClassLoaderContext);

            contextManager = new FilterContextManager(mockedFilterConfig);
            FilterContext filterContext = contextManager.loadFilterContext(filter, loadedApplications);

            assertNotNull(filterContext);
        }

        @Test
        public void shouldHandleNullFilterNameOnLoadFilterContext() throws ClassNotFoundException, FilterInitializationException {
            EarClassLoaderContext mockedEarClassLoaderContextWithNullClassName = getMockedEarClassLoader(null, false);
            EarClassLoaderContext mockedEarClassLoaderContextWithClassName = getMockedEarClassLoader("FilterClassName", false);

            Collection<EarClassLoaderContext> loadedApplications = new LinkedList<EarClassLoaderContext>();
            loadedApplications.add(mockedEarClassLoaderContextWithNullClassName);
            loadedApplications.add(mockedEarClassLoaderContextWithClassName);

            contextManager = new FilterContextManager(mockedFilterConfig);
            FilterContext filterContext = contextManager.loadFilterContext(filter, loadedApplications);

            assertNotNull(filterContext);
        }

        @Test(expected=FilterInitializationException.class)
        public void shouldThrowFilterInitializationExceptionOnLoadFilterContext() throws ClassNotFoundException, FilterInitializationException {
            EarClassLoaderContext mockedEarClassLoaderContext = getMockedEarClassLoader("FilterClassName", true);
            Collection<EarClassLoaderContext> loadedApplications = new LinkedList<EarClassLoaderContext>();
            loadedApplications.add(mockedEarClassLoaderContext);

            contextManager = new FilterContextManager(mockedFilterConfig);
            contextManager.loadFilterContext(filter, loadedApplications);
        }
    }
}
