package com.rackspace.papi.commons.util.logging.apache.format.stock;

import java.util.Collection;
import java.util.Enumeration;

public abstract class HeaderHandler {

   protected String getValues(Enumeration<String> values) {
      StringBuilder builder = new StringBuilder();
      boolean first = true;

      while (values != null && values.hasMoreElements()) {
         if (!first) {
            builder.append(",");
         }
         builder.append(values.nextElement());
         first = false;
      }
      return builder.toString();
   }

   protected String getValues(Collection<String> values) {
      StringBuilder builder = new StringBuilder();
      boolean first = true;

      if (values != null) {
         for (String value : values) {
            if (!first) {
               builder.append(",");
            }
            builder.append(value);
            first = false;
         }
      }

      return builder.toString();
   }
}
