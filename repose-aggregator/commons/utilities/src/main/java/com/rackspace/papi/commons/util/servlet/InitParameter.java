package com.rackspace.papi.commons.util.servlet;

public enum InitParameter {

   APP_CONTEXT_ADAPTER_CLASS("context-adapter-class");
   private final String initParameterName;

   private InitParameter(String webXmlName) {
      this.initParameterName = webXmlName;
   }

   public String getParameterName() {
      return initParameterName;
   }
}
