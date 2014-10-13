package org.openrepose.commons.util.test.mocks;

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
