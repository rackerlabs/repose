package com.rackspace.papi.security;

import com.rackspace.papi.security.permissions.file.FilePermissionBuilder;
import java.security.PermissionCollection;

/**
 *
 * @author zinic
 */
public class DefaultSecurityPermissions {

   public static PermissionCollection buildDefaultPermissions() {
      final SecurityPermissions permissions = new SecurityPermissions();

      permissions.addAll(FilePermissionBuilder.getInstance().getDefaultFilePermissions());

      return permissions;
   }
}
