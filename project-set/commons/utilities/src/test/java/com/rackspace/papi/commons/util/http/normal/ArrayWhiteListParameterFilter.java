package com.rackspace.papi.commons.util.http.normal;

import java.util.Arrays;

/**
 *
 * @author zinic
 */
public class ArrayWhiteListParameterFilter implements ParameterFilter {

   private final String[] whiteList;

   public ArrayWhiteListParameterFilter(String[] whiteList) {
      this.whiteList = Arrays.copyOf(whiteList, whiteList.length);;
   }

   @Override
   public boolean shouldAccept(String name) {
      for (String whitelistName : whiteList) {
         if (name.equals(whitelistName)) {
            return true;
         }
      }

      return false;
   }
}
