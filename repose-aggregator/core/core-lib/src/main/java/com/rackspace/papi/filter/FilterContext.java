package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.Destroyable;
import java.util.regex.Pattern;

import javax.servlet.Filter;

public class FilterContext implements Destroyable {

   private final ClassLoader filterClassLoader;
   private final Filter filter;
   private final com.rackspace.papi.model.Filter filterConfig;
   private final String name;
   private final String regex;
   private final Pattern pattern;

   public FilterContext(Filter filter, ClassLoader filterClassLoader) {
      this(filter, filterClassLoader, null);
   }

   public FilterContext(Filter filter, ClassLoader filterClassLoader, com.rackspace.papi.model.Filter filterConfig) {
      this.filter = filter;
      this.filterClassLoader = filterClassLoader;
      this.filterConfig = filterConfig;
      if (filterConfig != null && filterConfig.getUriRegex() != null) {
         filterConfig.getName();
         this.name = filterConfig.getName();
         this.regex = filterConfig.getUriRegex();
         this.pattern = Pattern.compile(regex);
      } else {
         this.name = "n/a";
         this.regex = ".*";
         this.pattern = Pattern.compile(this.regex);
      }

   }

   public Filter getFilter() {
      return filter;
   }

   public ClassLoader getFilterClassLoader() {
      return filterClassLoader;
   }

   public com.rackspace.papi.model.Filter getFilterConfig() {
      return filterConfig;
   }

   public Pattern getUriPattern() {
      return pattern;
   }
   
   public String getName() {
      return name;
   }
   
   public boolean isFilterAvailable() {
      return filter != null;
   }

   public String getUriRegex() {
      return regex;
   }

   @Override
   public void destroy() {
      if (filter != null) {
         filter.destroy();
      }
   }
}