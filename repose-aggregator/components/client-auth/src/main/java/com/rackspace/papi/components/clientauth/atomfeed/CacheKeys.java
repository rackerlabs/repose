package com.rackspace.papi.components.clientauth.atomfeed;

import java.util.Set;

public interface CacheKeys {

   void addTokenKey(String key);

   void addUserKey(String key);

   Set<String> getTokenKeys();

   Set<String> getUserKeys();
   
}
