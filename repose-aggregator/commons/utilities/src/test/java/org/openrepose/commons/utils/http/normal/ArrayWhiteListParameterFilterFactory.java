package org.openrepose.commons.utils.http.normal;

import java.util.Arrays;

/**
 *
 * @author zinic
 */
public class ArrayWhiteListParameterFilterFactory implements ParameterFilterFactory {

   private final String[] whiteList;

   public ArrayWhiteListParameterFilterFactory(String[] whiteList) {
      this.whiteList = Arrays.copyOf(whiteList, whiteList.length);
   }

   @Override
   public ParameterFilter newInstance() {
      return new ArrayWhiteListParameterFilter(whiteList);
   }
}
