package com.rackspace.papi.components.exceptionator;

import com.rackspace.papi.components.exceptionator.ExceptionatorFilter;
import com.rackspace.papi.components.exceptionator.IrishMythicalHeroException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 7/28/11
 * Time: 12:58 PM
 */
@RunWith(Enclosed.class)
public class ExceptionatorFilterTest {
    public static class WhenGeneratingExceptions {
        private ExceptionatorFilter filter;
        private HttpServletRequest httpServletRequest;

        @Before
        public void setup() {
            filter = new ExceptionatorFilter();
            httpServletRequest = mock(HttpServletRequest.class);
        }

        @Test
        public void shouldDoNothingOfDestroy() {
            filter.destroy();
        }

        @Test
        public void shouldDoNothingOnInit() throws ServletException {
            filter.init(null);
        }

        @Test
        public void shouldThrowExceptionIfMessageIsPresent() throws ClassNotFoundException, IOException, ServletException {
            String original, actual;
            original = "this is the test message";
            actual = null;

            when(httpServletRequest.getHeader(ExceptionatorFilter.EXCEPTION_MESSAGE_HEADER))
                    .thenReturn(original);

            try {
                filter.doFilter(httpServletRequest, null, null);
            } catch(IrishMythicalHeroException e) {
                actual = e.getMessage();
            }

            assertEquals(original, actual);
        }

        @Test
        public void shouldNotThrowExceptionIfMessageIsBlank() throws IOException, ServletException {
            when(httpServletRequest.getHeader(ExceptionatorFilter.EXCEPTION_MESSAGE_HEADER))
                    .thenReturn(null);

            filter.doFilter(httpServletRequest, null, null);
        }
    }
}
