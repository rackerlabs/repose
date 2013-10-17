package com.rackspace.components.compression.util;


public enum CompressionParameters {
   
   DEBUG("debug"),
   COMPRESSION_THRESHHOLD("compressionThreshold"),
   STATS_ENABLED("statsEnabled"),
   INCLUDE_CONTENT_TYPES("includeContentTypes"),
   EXCLUDE_CONTENT_TYPES("excludeContentTypes"),
   INCLUDE_PATH_PATTERNS("includePathPatterns"),
   EXCLUDE_PATH_PATTERNS("excludePathPatterns"),
   INCLUDE_USER_AGENT_PATTERNS("includUserAgentPatterns"),
   EXCLUDE_USER_AGENT_PATTERNS("excludeUserAgentPatterns"),
   JAVA_UTIL_LOGGER("javaUtilLogger"),
   JAKARTA_COMMONS_LOGGER("jakartaCommonsLogger");
   
   private final String param;

   private CompressionParameters(String param) {
      this.param = param;
   }
   
   public String getParam(){
      return param;
   }
}
