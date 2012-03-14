package com.rackspace.papi.commons.config;

public class ConfigurationResourceException extends RuntimeException {

   public ConfigurationResourceException(String string) {
      super(string);
   }

   public ConfigurationResourceException(String string, Throwable thrwbl) {
      super(string, thrwbl);
   }
}
