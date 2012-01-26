package com.rackspace.papi.commons.util.io;

import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class BufferCapacityExceptionTest {
   
   public static class WhenCreatingInstances {
      @Test
      public void shouldPreserveThrowable() {
         Throwable cause = new Exception("I am some trouble maker");
         BufferCapacityException exception = new BufferCapacityException("Message", cause);
         
         assertEquals(cause, exception.getCause());
      }
   }
   
}
