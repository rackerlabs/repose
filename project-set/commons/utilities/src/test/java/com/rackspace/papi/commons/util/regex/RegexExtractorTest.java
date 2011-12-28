package com.rackspace.papi.commons.util.regex;


import com.rackspace.papi.commons.util.regex.RegexExtractor;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class RegexExtractorTest {

   public static class WhenExtractingPatterns {

      @Test
      public void shouldReturnFirstCaptureGroup() {
         final RegexExtractor extractor = new RegexExtractor();
         extractor.addPattern("a([^z]+)z");
         
         assertEquals("Extractor should always return the first capture group when find(...) returns true", "bcd", extractor.extract("abcdz"));
      }
   }
}
