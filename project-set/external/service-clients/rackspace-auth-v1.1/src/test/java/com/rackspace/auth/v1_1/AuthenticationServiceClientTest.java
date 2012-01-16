package com.rackspace.auth.v1_1;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fran
 */
@Ignore
@RunWith(Enclosed.class)
public class AuthenticationServiceClientTest {

    public static class WhenGettingExpireTtl {
        private final long NUMBER_OF_MILLISECONDS_IN_A_SECOND = 1000;
        private final static long MOCK_CURRENT_SYS_TIME = 1000;
        private Calendar expirationTime;
        private Calendar currentSystemTime;

        @Before
        public void setup() {
            expirationTime = mock(GregorianCalendar.class);
            currentSystemTime = (mock(Calendar.class));

            when(currentSystemTime.getTimeInMillis()).thenReturn(MOCK_CURRENT_SYS_TIME);
        }

        // TODO These tests needs to be moved to CachableTokenInfoTest
        /*
        @Test
        public void shouldReturnMaxJavaInt() throws DatatypeConfigurationException {

            long mockExpirationTime = (MOCK_CURRENT_SYS_TIME + Integer.MAX_VALUE + 1l) * NUMBER_OF_MILLISECONDS_IN_A_SECOND;
            when(expirationTime.getTimeInMillis()).thenReturn(mockExpirationTime);

            assertEquals(Integer.MAX_VALUE, AuthenticationServiceClient.getTtl((GregorianCalendar)expirationTime, currentSystemTime));
        }

        @Test
        public void shouldReturnPositiveTime() throws DatatypeConfigurationException {

            long mockExpirationTime = MOCK_CURRENT_SYS_TIME + Integer.MAX_VALUE + 1l;
            when(expirationTime.getTimeInMillis()).thenReturn(mockExpirationTime);

            assertTrue(AuthenticationServiceClient.getTtl((GregorianCalendar)expirationTime, currentSystemTime) > 0);
        }               
        * 
        */
    }
}
