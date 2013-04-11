package com.rackspace.repose.service.ratelimit;

import org.slf4j.Logger;

import java.util.regex.Matcher;


public class LimitKey {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LimitKey.class);

   public LimitKey() {
   }

   public String getLimitKey(String uri, Matcher uriMatcher, boolean useCaptureGroups) {
      // The group count represents the number of elments that will go into
      // generating the unique cache id for the requested URI
      final int groupCount = uriMatcher.groupCount();

      final StringBuilder cacheIdBuffer = new StringBuilder();

      // Do we have any groups to use for generating our cache ID?
      if (groupCount > 0) {

         // Since these are regex groups we start at 1 since regex group 0 always
         // stands for the entire expression
       if(useCaptureGroups){   
         for (int i = 1; i <= groupCount; i++) {
           
                cacheIdBuffer.append(uriMatcher.group(i));
         }  
         }else{
                cacheIdBuffer.append(uriMatcher.pattern().toString());
             }
      
      } else {
         // We default to the whole URI in the case where no regex group info was provided
         LOG.warn("Using regex capture groups is recommended to help Repose build replicable, meaningful cache IDs for rate limits. Please update your config.");
         cacheIdBuffer.append(uri);
      }

      return cacheIdBuffer.toString();
   }
}
