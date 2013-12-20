package com.rackspace.papi.commons.util.regex;

/**
 *
 * @author zinic
 */
public class SelectorResult<K> {

   private static final SelectorResult EMPTY_INSTANCE = new SelectorResult(null);
   
   public static SelectorResult emptyResult() {
      return EMPTY_INSTANCE;
   }
   
   private final K key;

   public SelectorResult(K key) {
      this.key = key;
   }

   public boolean hasKey() {
      return key != null;
   }

   public K getKey() {
      return key;
   }
}
