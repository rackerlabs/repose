package com.rackspace.papi.commons.collections;

import java.util.Enumeration;
import java.util.Iterator;

/**
 *
 * @author zinic
 */
public class EnumerationIterable<T> implements Iterable<T> {

   private final EnumerationIterator<T> enumerationIterator;

   public EnumerationIterable(Enumeration<T> enumeration) {
      this.enumerationIterator = new EnumerationIterator<T>(enumeration);
   }

   @Override
   public Iterator<T> iterator() {
      return enumerationIterator;
   }
}
