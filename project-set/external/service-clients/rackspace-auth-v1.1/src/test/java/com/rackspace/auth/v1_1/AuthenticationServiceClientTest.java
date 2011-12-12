package com.rackspace.auth.v1_1;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class AuthenticationServiceClientTest {

    public static class WhenGettingExpireTtl {
        @Test
        public void shouldReturnMaxJavaInt() throws DatatypeConfigurationException {
            final long MOCK_CURRENT_SYS_TIME = 1000;
            final long NUMBER_OF_MILLISECONDS_IN_A_SECOND = 1000;
            final long long_one = 1;

            final Calendar expirationTime = mock(GregorianCalendar.class);
            long mockExpirationTime = MOCK_CURRENT_SYS_TIME;
            mockExpirationTime += Integer.MAX_VALUE + long_one;
            mockExpirationTime *= NUMBER_OF_MILLISECONDS_IN_A_SECOND;

            when(expirationTime.getTimeInMillis()).thenReturn(mockExpirationTime);

            final Calendar currentSystemTime = (mock(Calendar.class));
            when(currentSystemTime.getTimeInMillis()).thenReturn(MOCK_CURRENT_SYS_TIME);

            assertEquals(Integer.MAX_VALUE, AuthenticationServiceClient.getTtl((GregorianCalendar)expirationTime, currentSystemTime));
        }

        @Test
        public void shouldReturnPositiveTime() throws DatatypeConfigurationException {
            final long MOCK_CURRENT_SYS_TIME = 1000;
            final long long_one = 1;

            final Calendar expirationTime = mock(GregorianCalendar.class);
            long mockExpirationTime = MOCK_CURRENT_SYS_TIME;
            mockExpirationTime += Integer.MAX_VALUE + long_one;
            when(expirationTime.getTimeInMillis()).thenReturn(mockExpirationTime);

            final Calendar currentSystemTime = (mock(Calendar.class));
            when(currentSystemTime.getTimeInMillis()).thenReturn(MOCK_CURRENT_SYS_TIME);

            assertTrue(AuthenticationServiceClient.getTtl((GregorianCalendar)expirationTime, currentSystemTime) > 0);
        }
    }


}
