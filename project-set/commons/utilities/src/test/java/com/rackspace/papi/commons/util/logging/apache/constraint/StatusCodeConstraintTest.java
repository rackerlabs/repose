package com.rackspace.papi.commons.util.logging.apache.constraint;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 7/25/11
 * Time: 1:46 PM
 */
@RunWith(Enclosed.class)
public class StatusCodeConstraintTest {
    private static final Boolean INCLUSIVE_PASS = true;
    private static final Integer EXISTENT_STATUS_CODE = 42;
    private static final Integer NON_EXISTENT_STATUS_CODE = 101;

    public static class WhenUsingExclusivePass {
        private StatusCodeConstraint statusCodeConstraint;
        private HttpServletResponse response;

        @Before
        public void setup() {
            statusCodeConstraint = new StatusCodeConstraint(!INCLUSIVE_PASS);
            statusCodeConstraint.addStatusCode(EXISTENT_STATUS_CODE);

            response = mock(HttpServletResponse.class);
        }

        @Test
        public void shouldReturnFalseWhenCodeExists() {
            when(response.getStatus()).thenReturn(EXISTENT_STATUS_CODE);

            assertFalse(statusCodeConstraint.pass(response));
        }

        @Test
        public void shouldReturnTrueWhenNotCodeExists() {
            when(response.getStatus()).thenReturn(NON_EXISTENT_STATUS_CODE);

            assertTrue(statusCodeConstraint.pass(response));
        }
    }

    public static class WhenUsingInclusivePass {
        private StatusCodeConstraint statusCodeConstraint;
        private HttpServletResponse response;

        @Before
        public void setup() {
            statusCodeConstraint = new StatusCodeConstraint(INCLUSIVE_PASS);
            statusCodeConstraint.addStatusCode(EXISTENT_STATUS_CODE);

            response = mock(HttpServletResponse.class);
        }

        @Test
        public void shouldReturnTrueWhenCodeExists() {
            when(response.getStatus()).thenReturn(EXISTENT_STATUS_CODE);

            assertTrue(statusCodeConstraint.pass(response));
        }

        @Test
        public void shouldReturnFalseWhenNotCodeExists() {
            when(response.getStatus()).thenReturn(NON_EXISTENT_STATUS_CODE);

            assertFalse(statusCodeConstraint.pass(response));
        }
    }
}
