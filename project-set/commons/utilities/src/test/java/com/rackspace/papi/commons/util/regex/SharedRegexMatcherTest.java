package com.rackspace.papi.commons.util.regex;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * 
 */
@RunWith(Enclosed.class)
public class SharedRegexMatcherTest {
   public static class WhenSplittingStrings {
      private final SharedRegexMatcher splitter = new SharedRegexMatcher(",");
      private final String simple = "1,2,3,4,5";
      private final String complex = ",,2,3,4,5,";

      @Test
      public void shouldProduceAccurateResultsWithSimpleStringSplitting() {
         final String[] expected = simple.split(",");
         final String[] actual = splitter.split(simple);

         assertEquals(expected.length, actual.length);
      }

      @Test
      public void shouldProduceAccurateResultsWithComplexStringSplitting() {
         final String[] expected = complex.split(",");
         final String[] actual = splitter.split(complex);

         assertEquals(expected.length, actual.length);
      }
   }
}
