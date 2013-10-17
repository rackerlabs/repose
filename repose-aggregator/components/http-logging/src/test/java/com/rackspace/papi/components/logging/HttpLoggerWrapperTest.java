package com.rackspace.papi.components.logging;

import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.components.logging.util.SimpleLogger;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
        
@RunWith(Enclosed.class)
public class HttpLoggerWrapperTest {

   public static class WhenHandlingRequests {
      private HttpLoggerWrapper wrapper;
      private SimpleLogger logger;
      private String formatString = "some log string";
      
      @Before
      public void setUp() {
        
        wrapper = new HttpLoggerWrapper(new HttpLogFormatter(formatString));
        logger = mock(SimpleLogger.class);
        wrapper.addLogger(logger);
      }

      @Test
      public void shouldCallLog() {
         wrapper.handle(null, null);
         verify(logger, times(1)).log(formatString);
      }
      
      @Test
      public void shouldCallLogOncePerRequestHandled() {
         int expected = 10;
         for (int i = 0; i < expected; i++) {
            wrapper.handle(null, null);
         }
         verify(logger, times(expected)).log(eq(formatString));
      }
      
      @Test
      public void shouldHaveFormatter() {
         assertNotNull("Should have a formatter", wrapper.getFormatter());
      }
   }

   public static class WhenDestroying {
      private HttpLoggerWrapper wrapper;
      private SimpleLogger logger;
      private String formatString = "some log string";
      
      @Before
      public void setUp() {
        
        wrapper = new HttpLoggerWrapper(new HttpLogFormatter(formatString));
        logger = mock(SimpleLogger.class);
        wrapper.addLogger(logger);
      }

      @Test
      public void shouldCallDestroyOnLogger() {
         wrapper.destroy();
         verify(logger, times(1)).destroy();
      }
   }
}
