package com.rackspace.papi.commons.util.regex;


import com.rackspace.papi.commons.util.regex.RegexExtractor;
import java.util.regex.Pattern;
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

         String pattern = "a([^z]+)z";
         Pattern pat = Pattern.compile(pattern);
         extractor.addPattern(pattern);
         
         ExtractorResult result = extractor.extract("abcdz");
         assertEquals("Extractor should always return the first capture group when find(...) returns true", "bcd", result.getResult());
         assertEquals("Extractor should return matched pattern", pat.pattern(), result.getPattern().pattern());
      }
   }
}
