package com.rackspace.papi.commons.util.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author zinic
 */
public class SelectorPattern<K> {

   private final Pattern pattern;
   private final K key;

   public SelectorPattern(Pattern pattern, K key) {
      this.pattern = pattern;
      this.key = key;
   }

    public Pattern getPattern() {
        return pattern;
    }

   public K getKey() {
      return key;
   }

   public Matcher matcher(String target) {
      return pattern.matcher(target);
   }
}
