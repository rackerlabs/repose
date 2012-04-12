package com.rackspace.papi.commons.util.classloader.ear;

import com.oracle.javaee6.FilterType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EarDescriptor {

   private final Map<String, FilterType> registeredFilters;
   private String applicationName;

   EarDescriptor() {
      applicationName = "";
      registeredFilters = new HashMap<String, FilterType>();
   }

   void setApplicationName(String applicationName) {
      this.applicationName = applicationName;
   }

   Map<String, FilterType> getRegisteredFiltersMap() {
      return registeredFilters;
   }

   public String getApplicationName() {
      return applicationName;
   }

   public Map<String, FilterType> getRegisteredFilters() {
      return Collections.unmodifiableMap(registeredFilters);
   }
}
