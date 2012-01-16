package org.openrepose.components.rackspace.authz.cache;

import java.io.Serializable;

/**
 *
 * @author zinic
 */
public final class CachedEndpoint implements Serializable {

   private final String publicUrl;

   public CachedEndpoint(String publicUrl) {
      this.publicUrl = publicUrl;
   }

   public String getPublicUrl() {
      return publicUrl;
   }
}
