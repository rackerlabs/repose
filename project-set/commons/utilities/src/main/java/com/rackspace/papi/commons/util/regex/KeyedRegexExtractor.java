package com.rackspace.papi.commons.util.regex;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zinic
 */
public class KeyedRegexExtractor<K> {

   private final List<SelectorPattern<K>> compiledPatterns;

   public KeyedRegexExtractor() {
      compiledPatterns = new LinkedList<SelectorPattern<K>>();
   }

   public void clear() {
      compiledPatterns.clear();
   }

   public void addPattern(String regexString) {
      compiledPatterns.add(new SelectorPattern<K>(Pattern.compile(regexString), null));
   }

   public void addPattern(String regexString, K key) {
      compiledPatterns.add(new SelectorPattern<K>(Pattern.compile(regexString), key));
   }

   public ExtractorResult<K> extract(String target) {
      for (SelectorPattern<K> selector : compiledPatterns) {
         final Matcher matcher = selector.matcher(target);

         if (matcher.find() && matcher.groupCount() > 0) {
            return new ExtractorResult<K>(matcher.group(1), selector.getKey());
         }
      }

      return null;
   }
}
