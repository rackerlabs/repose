package org.openrepose.components.rackspace.authz.cache;

import java.io.Serializable;

/**
 *
 * @author zinic
 */
public final class CachedEndpoint implements Serializable {

   private final String publicUrl;
   private final String region;
   private final String name;
   private final String type;

   public CachedEndpoint(String publicUrl, String region, String name, String type) {
      this.publicUrl = publicUrl;
      this.region = region;
      this.name = name;
      this.type = type;
   }

   public String getPublicUrl() {
      return publicUrl;
   }

   public String getRegion() {
      return region;
   }

   public String getName() {
      return name;
   }

   public String getType() {
      return type;
   }
}
