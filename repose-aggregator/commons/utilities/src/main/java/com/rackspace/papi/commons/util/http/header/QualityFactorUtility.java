package com.rackspace.papi.commons.util.http.header;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @deprecated @see(com.rackspace.papi.commons.util.http.header.QualityFactorHeaderChooser)
 * 
 * @author zinic
 */
@Deprecated
public final class QualityFactorUtility {

   private QualityFactorUtility() {
   }

   public static <T extends HeaderValue> T choosePreferredHeaderValue(Iterable<T> headerValues) {
      final Iterator<T> headerValueIterator = headerValues != null ? headerValues.iterator() : Collections.EMPTY_LIST.iterator();

      T prefered = headerValueIterator.hasNext() ? headerValueIterator.next() : null;

      while (headerValueIterator.hasNext()) {
         final T next = headerValueIterator.next();

         if (next != null) {
            prefered = prefered.compareTo(next) < 0 ? next : prefered;
         }
      }

      return prefered;
   }

   public static <T extends HeaderValue> List<T> choosePreferredHeaderValues(Iterable<T> headerValues) {
      final Iterator<T> headerValueIterator = headerValues != null ? headerValues.iterator() : Collections.EMPTY_LIST.iterator();

      List<T> preferred = new ArrayList<T>();

      double currentQuality = -1;

      while (headerValueIterator.hasNext()) {
         final T next = headerValueIterator.next();

         if (next.getQualityFactor() > currentQuality) {
            preferred.clear();
            preferred.add(next);
            currentQuality = next.getQualityFactor();
         } else if (next.getQualityFactor() == currentQuality) {
            preferred.add(next);
         }
      }

      return preferred;
   }
}
