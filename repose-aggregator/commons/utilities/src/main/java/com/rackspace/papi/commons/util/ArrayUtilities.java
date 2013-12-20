package com.rackspace.papi.commons.util;

import java.util.Arrays;

public final class ArrayUtilities {

   private ArrayUtilities() {
   }

   public static <T> T[] nullSafeCopy(T[] array) {
      return array != null ? Arrays.copyOf(array, array.length) : null;
   }

   public static byte[] nullSafeCopy(byte[] array) {
      return array != null ? Arrays.copyOf(array, array.length) : null;
   }
}
