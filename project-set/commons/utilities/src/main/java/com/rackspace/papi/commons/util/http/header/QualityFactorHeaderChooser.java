package com.rackspace.papi.commons.util.http.header;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author zinic
 */
public class QualityFactorHeaderChooser<T extends HeaderValue> implements HeaderChooser<T> {

   private final T defaultValue;

   public QualityFactorHeaderChooser(T defaultValue) {
      this.defaultValue = defaultValue;
   }

   @Override
   public T choosePreferredHeaderValue(Iterable<T> headerValues) {
      final Iterator<T> headerValueIterator = headerValues != null ? headerValues.iterator() : Collections.EMPTY_LIST.iterator();

      T prefered = headerValueIterator.hasNext() ? headerValueIterator.next() : defaultValue;

      while (headerValueIterator.hasNext()) {
         final T next = headerValueIterator.next();

         if (next != null) {
            prefered = prefered.compareTo(next) < 0 ? next : prefered;
         }
      }

      return prefered;
   }

   @Override
   public List<T> choosePreferredHeaderValues(Iterable<T> headerValues) {
      final Iterator<T> headerValueIterator = headerValues != null ? headerValues.iterator() : Collections.EMPTY_LIST.iterator();
      final List<T> preferredHeaders = new ArrayList<T>();

      double currentQuality = -1;

      while (headerValueIterator.hasNext()) {
         final T next = headerValueIterator.next();

         if (next.getQualityFactor() > currentQuality) {
            preferredHeaders.clear();
            preferredHeaders.add(next);
            currentQuality = next.getQualityFactor();
         } else if (next.getQualityFactor() == currentQuality) {
            preferredHeaders.add(next);
         }
      }

      if (preferredHeaders.isEmpty()) {
         preferredHeaders.add(defaultValue);
      }

      return preferredHeaders;
   }
}
