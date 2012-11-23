package com.rackspace.papi.security;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;

/**
 *
 * @author zinic
 */
public interface MergablePermissions {

   void add(Permission newPermission);

   void addAll(PermissionCollection permissionsToMerge);

   Enumeration<Permission> elements();

   boolean implies(Permission permission);
   
}
