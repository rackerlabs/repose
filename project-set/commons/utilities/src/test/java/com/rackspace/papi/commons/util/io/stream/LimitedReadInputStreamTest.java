package com.rackspace.papi.commons.util.io.stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class LimitedReadInputStreamTest {

   public static class WhenReading {

      protected InputStream mockedInputStream;
      
      @Before
      public void standUp() throws Exception {
         mockedInputStream = mock(InputStream.class);
         when(mockedInputStream.read()).thenReturn(1);
      }

      @Test
      public void shouldAllowReading() throws Exception {
         final LimitedReadInputStream stream = new LimitedReadInputStream(10, mockedInputStream);
         
         assertEquals("LimitedReadInputStream must delegate reads to the wrapped InputStream", 1, stream.read());
      }

      @Test(expected = ReadLimitReachedException.class)
      public void shouldHaltReadingWhenLimitIsBreached() throws Exception {
         final LimitedReadInputStream stream = new LimitedReadInputStream(0, mockedInputStream);
         stream.read();
      }
   }
}
