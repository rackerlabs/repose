package com.rackspace.papi.commons.util;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 *
 */
@RunWith(Enclosed.class)
public class StringUtilitiesTest {

   public static class WhenJoiningStrings {

      @Test
      public void shouldJoinTwoStrings() {
         String expected, actual;

         expected = "thing one, thing two";
         actual = StringUtilities.join("thing one, ", "thing two");

         assertEquals(expected, actual);
      }

      @Test
      public void shouldManyThings() {
         String expected, actual;

         expected = "1duck2.5";
         actual = StringUtilities.join(1, "duck", 2.5);

         assertEquals(expected, actual);
      }
   }

   public static class WhenCheckingIfAStringIsBlank {

      @Test
      public void shouldHandleNulls() {
         assertTrue(StringUtilities.isBlank(null));
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

   public static class WhenCheckingIfAStringIsNotBlank {

      @Test
      public void shouldHandleNulls() {
         assertFalse(StringUtilities.isNotBlank(null));
      }

      @Test
      public void shouldHandleEmptyStrings() {
         assertFalse(StringUtilities.isNotBlank(null));
      }

      @Test
      public void shouldHandleBlankStrings() {
         assertFalse(StringUtilities.isNotBlank("     "));
      }

      @Test
      public void shouldHandleBlankStringsWithNewLines() {
         assertFalse(StringUtilities.isNotBlank("\n\n"));
      }

      @Test
      public void shouldHandleBlankStringsWithTabs() {
         assertFalse(StringUtilities.isNotBlank("\t\t"));
      }

      @Test
      public void shouldHandleComplexBlankStrings() {
         assertFalse(StringUtilities.isNotBlank("\n\n  \t  \t\n  \t\n   \n\t"));
      }

      @Test
      public void shouldRejectComplexNonBlankStrings() {
         assertTrue(StringUtilities.isNotBlank("\n\n  \t abc123 \t\n  \t\n   \n\t"));
      }

      @Test
      public void shouldRejectNonBlankStrings() {
         assertTrue(StringUtilities.isNotBlank("zf-adapter"));
      }
   }

   public static class WhenTrimmingStrings {

      public static final String TEST_STRING = "%testing!complexMatch",
              BEGINNING_TRIM = "%",
              END_TRIM = "!complexMatch";

      @Test
      public void shouldStripFromBeginningOfString() {
         final String actual = StringUtilities.trim(TEST_STRING, BEGINNING_TRIM);

         assertFalse(actual.contains(BEGINNING_TRIM));
         assertTrue(actual.length() == (TEST_STRING.length() - BEGINNING_TRIM.length()));
      }

      @Test
      public void shouldStripFromEndOfString() {
         final String actual = StringUtilities.trim(TEST_STRING, END_TRIM);

         assertFalse(actual.contains(END_TRIM));
         assertTrue(actual.length() == (TEST_STRING.length() - END_TRIM.length()));
      }

      @Test
      public void shouldNotStripIfMatchIsNotFound() {
         final String actual = StringUtilities.trim(TEST_STRING, "NEVER FIND ME");

         assertTrue(actual.length() == TEST_STRING.length());
      }

      @Test
      public void shouldDoNothingIfTrimCharsAreLongerThanTargetString() {
         String expected, actual;

         expected = "string";
         actual = StringUtilities.trim("string", "strings");

         assertEquals(expected, actual);
      }
   }

   public static class WhenPerformingNullSafeEquals {

      @Test
      public void shouldReturnFalseIfFirstStringIsNull() {
         String one = null;
         String two = "abc";

         assertFalse(StringUtilities.nullSafeEqualsIgnoreCase(one, two));
      }

      @Test
      public void shouldReturnFalseIfSecondStringIsNull() {
         String one = "abc";
         String two = null;

         assertFalse(StringUtilities.nullSafeEqualsIgnoreCase(one, two));
      }

      @Test
      public void shouldReturnFalseIfNonNullStringsAreDifferent() {
         String one = "abc";
         String two = "def";

         assertFalse(StringUtilities.nullSafeEqualsIgnoreCase(one, two));
      }

      @Test
      public void shouldReturnTrueIfBothStringsAreNull() {
         String one = null;
         String two = null;

         assertTrue(StringUtilities.nullSafeEqualsIgnoreCase(one, two));
      }

      @Test
      public void shouldReturnTrueIfNonNullStringsAreSame() {
         String one = "abc";
         String two = "AbC";

         assertTrue(StringUtilities.nullSafeEqualsIgnoreCase(one, two));
      }
   }

   public static class WhenPerformingNullSafeStartsWith {

      @Test
      public void shouldReturnTrueIfBothStringsAreBlank() {
         final String one = "";
         final String two = "";

         assertTrue(StringUtilities.nullSafeStartsWith(one, two));
      }

      @Test
      public void shouldReturnTrueIfFirstStringStartsWithSecondString() {
         final String one = "hello my friend hello";
         final String two = "hello";

         assertTrue(StringUtilities.nullSafeStartsWith(one, two));
      }

      @Test
      public void shouldReturnFalseIfFirstStringIsNull() {
         final String one = null;
         final String two = "hello";

         assertFalse(StringUtilities.nullSafeStartsWith(one, two));
      }

      @Test
      public void shouldReturnFalseIfSecondStringIsNull() {
         final String one = "hello";
         final String two = null;

         assertFalse(StringUtilities.nullSafeStartsWith(one, two));
      }

      @Test
      public void shouldReturnFalseIfBothStringsAreNull() {
         final String one = null;
         final String two = null;

         assertFalse(StringUtilities.nullSafeStartsWith(one, two));
      }

      @Test
      public void shouldReturnFalseIfFirstStringDoesNotStartWithSecondString() {
         final String one = "this is a tribute to neil diamond, hello my friend hello";
         final String two = "hello my friend hello";

         assertFalse(StringUtilities.nullSafeStartsWith(one, two));
      }
   }
}
