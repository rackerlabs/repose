package com.rackspace.papi.commons.util.regex;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class KeyedRegexExtractorTest {

   public static class WhenExtractingPatterns {

      @Test
      public void shouldReturnFirstCaptureGroup() {
         final KeyedRegexExtractor<Object> extractor = new KeyedRegexExtractor<Object>();
         final Object expectedKey = new Object();

         final String pattern = "a([^z]+)z";
         extractor.addPattern(pattern, expectedKey);

         ExtractorResult result = extractor.extract("abcdz");
         assertEquals("Extractor should always return the first capture group when find(...) returns true", "bcd", result.getResult());
         assertEquals("Extractor should return matched key", expectedKey, result.getKey());
      }

      @Test
      public void shouldUseNullKeys() {
         final KeyedRegexExtractor<Object> extractor = new KeyedRegexExtractor<Object>();

         final String pattern = "a([^z]+)z";
         extractor.addPattern(pattern);

         ExtractorResult result = extractor.extract("abcdz");
         assertEquals("Extractor should always return the first capture group when find(...) returns true", "bcd", result.getResult());
         assertNull("Extractor should return null keys when patterns are added without them", result.getKey());
      }
      
      @Test
      public void shouldCaptureUserWithNegativeId(){
          final KeyedRegexExtractor<Object> extractor = new KeyedRegexExtractor<Object>();
         final Object expectedKey = new Object();

         final String pattern = ".*/servers/([-|\\w]+)/?.*";
         extractor.addPattern(pattern, expectedKey);
         

         ExtractorResult result = extractor.extract("http://n01.repose.org/servers/-384904");
         assertEquals("Extractor should always return the first capture group when find(...) returns true", "-384904", result.getResult());
         assertEquals("Extractor should return matched key", expectedKey, result.getKey());
      }
   }
}
