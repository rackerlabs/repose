package com.rackspace.papi.security;

import java.security.Permission;
import java.security.PermissionCollection;

/**
 *
 * @author zinic
 */
public class DelegatingSecurityManager extends SecurityManager {

   private final PermissionCollection permissions;
   private final SecurityManager delegatedManager;

   public DelegatingSecurityManager(PermissionCollection permissions) {
      this(permissions, null);
   }

   public DelegatingSecurityManager(PermissionCollection permissions, SecurityManager delegatedManager) {
      this.permissions = permissions;
      this.delegatedManager = delegatedManager;
   }

   @Override
   public void checkPermission(Permission prmsn) {
      if (!permissions.implies(prmsn)) {
         if (delegatedManager != null) {
            delegatedManager.checkPermission(prmsn);
         } else {
            throw new SecurityException("Operation disallowed by current security policy. "
                    + "If you believe this to be an error, please submit the resulting stack trace as a bug.");
         }
      }
   }
}
