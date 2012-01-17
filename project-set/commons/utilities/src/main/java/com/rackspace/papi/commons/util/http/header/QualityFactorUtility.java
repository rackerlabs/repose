package com.rackspace.papi.commons.util.http.header;

import java.util.Collections;
import java.util.Iterator;

/**
 *
 * @author zinic
 */
public final class QualityFactorUtility {
   
   private QualityFactorUtility() {
   }
   
   public static <T extends HeaderValue> T choosePreferedHeaderValue(Iterable<T> headerValues) {
      final Iterator<T> headerValueIterator = headerValues != null ? headerValues.iterator() : Collections.EMPTY_LIST.iterator();
      
      T prefered = headerValueIterator.hasNext() ? headerValueIterator.next() : null;
      
      while (headerValueIterator.hasNext()) {
         final T next = headerValueIterator.next();
         
         if (next.getQualityFactor() > prefered.getQualityFactor()) {
            prefered = next;
         }
      }
      
      return prefered;
   }
}
