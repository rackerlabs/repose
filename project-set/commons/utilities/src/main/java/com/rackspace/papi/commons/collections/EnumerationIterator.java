package com.rackspace.papi.commons.collections;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author zinic
 */
public class EnumerationIterator<T> implements Iterator<T> {

   private final Enumeration<T> enumeration;

   public EnumerationIterator(Enumeration<T> enumeration) {
      this.enumeration = enumeration;
   }

   @Override
   public boolean hasNext() {
      return enumeration.hasMoreElements();
   }

   @Override
   public T next() {
      if (!enumeration.hasMoreElements()) {
         throw new NoSuchElementException();
      }

      return enumeration.nextElement();
   }

   @Override
   public void remove() {
      throw new UnsupportedOperationException("Enumerations do not support element removal.");
   }
}