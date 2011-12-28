package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class HeaderManagerImplTest {
    public static class WhenAppendingToHeader{

        private HeaderManagerImpl headerManagerImpl = new HeaderManagerImpl();
        private HttpServletRequest mockRequest = mock(HttpServletRequest.class);


        @Test
        public void shouldAppendWhenHeaderAlreadyPresent() {
            when(mockRequest.getHeader(PowerApiHeader.USER.getHeaderKey())).thenReturn("127.0.0.0;q=.3");

            headerManagerImpl.appendToHeader(mockRequest, PowerApiHeader.USER.getHeaderKey(), "username;q=1" );

            Set<String> values = headerManagerImpl.headersToAdd().get(PowerApiHeader.USER.getHeaderKey().toLowerCase());

            assertEquals("Should append header value if header already present.","127.0.0.0;q=.3,username;q=1", values.iterator().next());
        }

        @Test
        public void shouldPutHeaderWhenHeaderNotPresent() {
            when(mockRequest.getHeader(PowerApiHeader.USER.getHeaderKey())).thenReturn(null);

            headerManagerImpl.appendToHeader(mockRequest, PowerApiHeader.USER.getHeaderKey(), "username;q=1" );

            Set<String> values = headerManagerImpl.headersToAdd().get(PowerApiHeader.USER.getHeaderKey().toLowerCase());

            assertEquals("Should put header value if header not present.","username;q=1", values.iterator().next());
        }        
    }
}
