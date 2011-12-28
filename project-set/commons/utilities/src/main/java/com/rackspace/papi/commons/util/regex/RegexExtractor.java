package com.rackspace.papi.commons.util.regex;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zinic
 */
public class RegexExtractor {

   private final List<Pattern> compiledPatterns;

   public RegexExtractor() {
      compiledPatterns = new LinkedList<Pattern>();
   }

   public void addPattern(String regexString) {
      compiledPatterns.add(Pattern.compile(regexString));
   }

   public ExtractorResult extract(String target) {
      for (Pattern p : compiledPatterns) {
         final Matcher matcher = p.matcher(target);

         if (matcher.find() && matcher.groupCount() > 0) {
            return new ExtractorResult(p, matcher.group(1));
         }
      }

      return null;
   }
}
