package com.rackspace.papi.components.clientauth.common;

import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;

/**
 * @author fran
 */
public class Configurables {
   private final boolean delegable;
   private final String authServiceUri;
   private final KeyedRegexExtractor<String> keyedRegexExtractor;
   private final boolean  includeQueryParams, tenanted;

   public Configurables(boolean delegable, String authServiceUri, KeyedRegexExtractor<String> keyedRegexExtractor, boolean includeQueryParams, boolean tenanted) {
      this.delegable = delegable;
      this.authServiceUri = authServiceUri;
      this.keyedRegexExtractor = keyedRegexExtractor;
      this.includeQueryParams = includeQueryParams;
      this.tenanted = tenanted;
   }

   public boolean isDelegable() {
      return delegable;
   }

   public String getAuthServiceUri() {
      return authServiceUri;
   }

   public KeyedRegexExtractor<String> getKeyedRegexExtractor() {
      return keyedRegexExtractor;
   }

   public boolean isIncludeQueryParams() {
      return includeQueryParams;
   }
   
   public boolean isTenanted(){
       return tenanted;
   }
}
