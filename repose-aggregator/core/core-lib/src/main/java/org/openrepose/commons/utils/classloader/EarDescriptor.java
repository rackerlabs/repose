package org.openrepose.commons.utils.classloader;

import com.oracle.javaee6.FilterType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EarDescriptor {

   private final Map<String, FilterType> registeredFilters;
   private String applicationName;

   public EarDescriptor() {
      this("", new HashMap<String, FilterType>());
   }

   public EarDescriptor(String applicationName, Map<String, FilterType> registeredFilters){
      this.applicationName = applicationName;
      this.registeredFilters = registeredFilters;
   }

   public String getApplicationName() {
      return applicationName;
   }

   public Map<String, FilterType> getRegisteredFilters() {
      return Collections.unmodifiableMap(registeredFilters);
   }
}
