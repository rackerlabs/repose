package com.rackspace.papi.components.versioning;

import com.rackspace.papi.components.versioning.VersioningFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.FilterConfig;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Apr 28, 2011
 * Time: 3:30:13 PM
 */
@RunWith(Enclosed.class)
public class VersioningFilterTest {
    public static class When {
        private VersioningFilter filter;
        private FilterConfig filterConfig;

        @Before
        public void setup() {
            //TODO: fix?  bah!  code is missing for this sucka! (meaning it's missing for javax/servlet/ServletException)
            //filter = new VersioningFilter();

            filterConfig = null; //not used at this time
        }

        @Test
        public void should() {
            //filter.initialize(filterConfig);
        }
    }
}
