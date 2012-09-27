package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class HeaderManagerImplTest {

   public static class WhenAppendingToHeader {

      private HeaderManagerImpl headerManagerImpl = new HeaderManagerImpl();
      private HttpServletRequest mockRequest = mock(HttpServletRequest.class);

      @Test
      public void shouldAppendToHeadersAlreadyInSet() {
         final String key = "key";

         headerManagerImpl.putHeader(key, "1");
         headerManagerImpl.appendHeader(key, "2");

         final Iterator<String> valueIterator = headerManagerImpl.headersToAdd().get(key).iterator();
         final String firstValue = valueIterator.next();

         if ("1".equals(firstValue)) {
            assertEquals("Should append header values already present in the value set.", "2", valueIterator.next());
         } else if ("2".equals(firstValue)) {
            assertEquals("Should append header values already present in the value set.", "1", valueIterator.next());
         } else {
            fail("Header manager should allow values to be appended to the same header key");
         }
      }

      @Test
      public void shouldAppendWhenHeaderAlreadyPresent() {
         when(mockRequest.getHeader(PowerApiHeader.USER.toString())).thenReturn("127.0.0.0;q=.3");

         headerManagerImpl.appendToHeader(mockRequest, PowerApiHeader.USER.toString(), "username;q=1");

         Set<String> values = headerManagerImpl.headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());

         assertEquals("Should append header value if header already present.", "127.0.0.0;q=.3,username;q=1", values.iterator().next());
      }

      @Test
      public void shouldPutHeaderWhenHeaderNotPresent() {
         when(mockRequest.getHeader(PowerApiHeader.USER.toString())).thenReturn(null);

         headerManagerImpl.appendToHeader(mockRequest, PowerApiHeader.USER.toString(), "username;q=1");

         Set<String> values = headerManagerImpl.headersToAdd().get(PowerApiHeader.USER.toString().toLowerCase());

         assertEquals("Should put header value if header not present.", "username;q=1", values.iterator().next());
      }
   }
}
