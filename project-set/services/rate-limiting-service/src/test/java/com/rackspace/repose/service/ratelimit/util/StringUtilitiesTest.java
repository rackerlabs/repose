package com.rackspace.repose.service.ratelimit.util;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class StringUtilitiesTest {

   public static class WhenCheckingIfAStringIsBlank {

      @Test
      public void shouldHandleNulls() {
         assertTrue(com.rackspace.papi.commons.util.StringUtilities.isBlank(null));
      }

      @Test
      public void shouldHandleEmptyStrings() {
         assertTrue(StringUtilities.isBlank(null));
      }

      @Test
      public void shouldHandleBlankStrings() {
         assertTrue(StringUtilities.isBlank("     "));
      }

      @Test
      public void shouldHandleBlankStringsWithNewLines() {
         assertTrue(StringUtilities.isBlank("\n\n"));
      }

      @Test
      public void shouldHandleBlankStringsWithTabs() {
         assertTrue(StringUtilities.isBlank("\t\t"));
      }

      @Test
      public void shouldHandleComplexBlankStrings() {
         assertTrue(StringUtilities.isBlank("\n\n  \t  \t\n  \t\n   \n\t"));
      }

      @Test
      public void shouldRejectComplexNonBlankStrings() {
         assertFalse(StringUtilities.isBlank("\n\n  \t abc123 \t\n  \t\n   \n\t"));
      }

      @Test
      public void shouldRejectNonBlankStrings() {
         assertFalse(StringUtilities.isBlank("zf-adapter"));
      }
   }
}
