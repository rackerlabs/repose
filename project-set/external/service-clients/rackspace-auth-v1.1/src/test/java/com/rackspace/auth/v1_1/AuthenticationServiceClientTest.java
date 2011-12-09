package com.rackspace.auth.v1_1;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.Calendar;
import java.util.GregorianCalendar;

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
        @Ignore
        public void shouldReturnPositiveTime() throws DatatypeConfigurationException {

            final Calendar calendar = mock(GregorianCalendar.class);
            when(calendar.getTimeInMillis()).thenReturn(Calendar.getInstance().getTimeInMillis() + (Integer.MAX_VALUE + 1000));

            assertTrue(AuthenticationServiceClient.getTtl((GregorianCalendar)calendar) > 0);
        }        
    }


}
