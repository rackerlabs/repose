package com.rackspace.papi.commons.util.regex;

/**
 *
 * @author malconis
 */
public class ExtractorResult<K> {

   private final String result;
   private final K key;

   public ExtractorResult(String result, K key) {
      this.result = result;
      this.key = key;
   }

   public boolean hasKey() {
      return key != null;
   }

   public K getKey() {
      return key;
   }

   public boolean hasResult() {
      return result != null;
   }

   public String getResult() {
      return result;
   }
}
