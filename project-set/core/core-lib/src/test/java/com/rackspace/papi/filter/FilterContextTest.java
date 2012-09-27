package com.rackspace.papi.filter;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.Filter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class FilterContextTest {
    public static class WhenUsingFilterContextTest {
        @Test
        public void shouldInstantiate() {
            FilterContext filterContext = new FilterContext(null, null);

            assertNotNull(filterContext);
        }

        @Test
        public void shouldGetFilter() {
            FilterContext filterContext = new FilterContext(null, null);
            Filter filter = filterContext.getFilter();

            assertNull(filter);
        }

        @Test
        public void shouldGetClassLoader() {
            FilterContext filterContext = new FilterContext(null, null);
            ClassLoader classLoader = filterContext.getFilterClassLoader();

            assertNull(classLoader);
        }
    }
}
