package com.rackspace.repose.service.ratelimit.util;

import java.util.regex.Pattern;

public final class StringUtilities {

   private StringUtilities() {
   }

   private static final Pattern IS_BLANK_PATTERN = Pattern.compile("[\\s]*");

   public static boolean isEmpty(String st) {
      return st == null || st.length() == 0;
   }

   public static boolean isBlank(String st) {
      return isEmpty(st) || IS_BLANK_PATTERN.matcher(st).matches();
   }
}
