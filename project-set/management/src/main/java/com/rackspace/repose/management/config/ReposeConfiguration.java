package com.rackspace.repose.management.config;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 30, 2012
 * Time: 1:20:33 PM
 */
public enum ReposeConfiguration {

   RATE_LIMITING("rate-limiting.cfg.xml", "com.rackspace.repose.service.ratelimit.config"),
   NORMALIZATION("content-normalization.cfg.xml", "com.rackspace.papi.components.normalization.config"),
   VERSIONING("versioning.cfg.xml", "com.rackspace.papi.components.versioning.config"),
   SYSTEM("system-model.cfg.xml", "com.rackspace.papi.model"),
   CONTAINER("container.cfg.xml", "com.rackspace.papi.container.config"),
   RMS("response-messaging.cfg.xml", "com.rackspace.papi.service.rms.config"),
   UNKNOWN("unknown", "unknown");

   private String configFilename;
   private String configContextPath;

   private ReposeConfiguration(String filterName, String configContextPath) {
      this.configFilename = filterName;
      this.configContextPath = configContextPath;
   }

   public String getConfigFilename() {
      return configFilename;
   }

   public String getConfigContextPath() {
      return configContextPath;
   }
}
