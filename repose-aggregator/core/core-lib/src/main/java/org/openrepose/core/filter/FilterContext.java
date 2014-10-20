package org.openrepose.core.filter;

import org.openrepose.commons.utils.Destroyable;
import java.util.regex.Pattern;

import javax.servlet.Filter;

public class FilterContext implements Destroyable {

   private final ClassLoader filterClassLoader;
   private final Filter filter;
   private final org.openrepose.core.systemmodel.Filter filterConfig;
   private final String name;
   private final String regex;
   private final Pattern pattern;

   public FilterContext(Filter filter, ClassLoader filterClassLoader) {
      this(filter, filterClassLoader, null);
   }

   public FilterContext(Filter filter, ClassLoader filterClassLoader, org.openrepose.core.systemmodel.Filter filterConfig) {
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

   public org.openrepose.core.systemmodel.Filter getFilterConfig() {
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
       //TODO: maybe hand this guy the filter application context so it can be tanked as well
      if (filter != null) {
         filter.destroy();
      }
   }
}