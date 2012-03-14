package com.rackspace.papi.commons.util.regex;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zinic
 */
public class KeyedRegexExtractor<K> {

   private final Map<Pattern, K> compiledPatternMap;

   public KeyedRegexExtractor() {
      compiledPatternMap = new HashMap<Pattern, K>();
   }

   public void addPattern(String regexString) {
      compiledPatternMap.put(Pattern.compile(regexString), null);
   }

   public void addPattern(String regexString, K key) {
      compiledPatternMap.put(Pattern.compile(regexString), key);
   }

   public ExtractorResult<K> extract(String target) {
      for (Map.Entry<Pattern, K> patternToKeyEntry : compiledPatternMap.entrySet()) {
         final Matcher matcher = patternToKeyEntry.getKey().matcher(target);

         if (matcher.find() && matcher.groupCount() > 0) {
            return new ExtractorResult<K>(matcher.group(1), patternToKeyEntry.getValue());
         }
      }

      return null;
   }
}
