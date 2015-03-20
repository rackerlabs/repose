/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.commons.utils.test.mocks;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.test.mocks.DataProviderImpl;

import static org.junit.Assert.assertNotNull;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class DataProviderImplTest {

    public static class WhenGettingCalendars {

        private DataProviderImpl provider;
        
        @Before
        public void standUp() throws Exception {
            provider = new DataProviderImpl();
        }

        @Test
        public void shouldReturnValidXmlGregorianCalendar() {
            assertNotNull("Base resource must return valid XML Gregorian Calendar instances.", provider.getCalendar());
        }
    }
}
