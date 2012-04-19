package com.rackspace.papi.filter;

import com.rackspace.papi.security.DefaultSecurityPermissions;
import com.rackspace.papi.security.DelegatingSecurityManager;

/**
 *
 * @author zinic
 */
public final class SecurityManagerFactory {
    
    private SecurityManagerFactory(){
    }

   public static SecurityManager newDefaultSecurityManager() {
      final SecurityManager currentManager = System.getSecurityManager();

      return new DelegatingSecurityManager(DefaultSecurityPermissions.buildDefaultPermissions(), currentManager);
   }
}
