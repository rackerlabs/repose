package com.rackspace.papi.commons.util.regex;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author zinic
 */
public class RegexSelector<K> {

   private final List<SelectorPattern<K>> compiledPatterns;

   public RegexSelector() {
      compiledPatterns = new LinkedList<SelectorPattern<K>>();
   }

   public void clear() {
      compiledPatterns.clear();
   }

   public void addPattern(String pattern, K key) {
      compiledPatterns.add(new SelectorPattern<K>(Pattern.compile(pattern), key));
   }

   public SelectorResult<K> select(String selectOn) {
      for (SelectorPattern<K> selector : compiledPatterns) {
         if (selector.matcher(selectOn).matches()) {
            return new SelectorResult<K>(selector.getKey());
         }
      }

      return SelectorResult.emptyResult();
   }
}
