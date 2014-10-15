package org.openrepose.commons.utils.arrays;

import org.openrepose.commons.utils.ArrayUtilities;

public class ByteArrayComparator implements ArrayComparator {

   private final byte[] first, second;

   public ByteArrayComparator(byte[] first, byte[] second) {
      this.first = ArrayUtilities.nullSafeCopy(first);
      this.second = ArrayUtilities.nullSafeCopy(second);
   }

   @Override
   public boolean arraysAreEqual() {
      boolean same = first.length == second.length;

      if (same) {
         for (int i = 0; i < first.length && same; i++) {
            same = first[i] == second[i];
         }
      }

      return same;
   }
}
