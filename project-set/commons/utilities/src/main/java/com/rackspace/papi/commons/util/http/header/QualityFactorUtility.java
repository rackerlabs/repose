package com.rackspace.papi.commons.util.http.header;

import java.util.Iterator;

/**
 *
 * @author zinic
 */
public final class QualityFactorUtility {
   
   private QualityFactorUtility() {
   }
   
   public static HeaderValue choosePreferedHeaderValue(Iterable<HeaderValue> headerValues) {
      final Iterator<HeaderValue> headerValueIterator = headerValues.iterator();
      
      HeaderValue prefered = headerValueIterator.hasNext() ? headerValueIterator.next() : null;
      
      while (headerValueIterator.hasNext()) {
         final HeaderValue next = headerValueIterator.next();
         
         if (next.getQualityFactor() > prefered.getQualityFactor()) {
            prefered = next;
         }
      }
      
      return prefered;
   }
}
