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
package org.openrepose.commons.utils.logging.apache.constraint;

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
