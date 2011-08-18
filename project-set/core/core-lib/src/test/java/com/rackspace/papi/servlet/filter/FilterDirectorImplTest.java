package com.rackspace.papi.servlet.filter;

import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * @author jhopper
 */
@RunWith(Enclosed.class)
public class FilterDirectorImplTest {

    public static class WhenCreatingNewInstances {

        @Test
        public void shouldProvideNonNullDefaults() {
            final FilterDirectorImpl impl = new FilterDirectorImpl();
            
            assertNotNull("By default, the delegated action should not be null", impl.getFilterAction());
            assertNotNull("By default, the delegated status should not be null", impl.getResponseStatus());
            assertNotNull("By default, the message body should not be null", impl.getResponseMessageBody());
        }
    }
}
