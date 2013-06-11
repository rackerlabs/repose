/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.clientauth.atomfeed;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeedCacheKeys implements CacheKeys {
   
   
   private Map<CacheKeyType, Set<String>> keys;

   public FeedCacheKeys() {
      keys = new EnumMap(CacheKeyType.class);
      for(CacheKeyType type: CacheKeyType.values()){
         keys.put(type, new HashSet<String>());
      }
   }
   
   
   @Override
   public void addTokenKey(String key){
      keys.get(CacheKeyType.TOKEN).add(key);
   }
   
   @Override
   public void addUserKey(String key){
      keys.get(CacheKeyType.USER).add(key);
   }
   
   @Override
   public Set<String> getTokenKeys(){
      return keys.get(CacheKeyType.TOKEN);
   }
   
   
   @Override
   public Set<String> getUserKeys(){
      return keys.get(CacheKeyType.USER);
   }
}
