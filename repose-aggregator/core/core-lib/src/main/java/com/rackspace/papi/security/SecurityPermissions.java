package com.rackspace.papi.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.*;

/**
 * This is a collection class for Permissions that allows the merging of
 * Permission sets.
 *
 * @author zinic
 */
public class SecurityPermissions extends PermissionCollection implements MergablePermissions {

   private static final Logger LOG = LoggerFactory.getLogger(SecurityPermissions.class);
   private final Set<Permission> permissions;

   public SecurityPermissions() {
      permissions = new HashSet<Permission>();
   }

   @Override
   public void add(Permission newPermission) {
      final List<Permission> permissionImplicators = getPermissionsThatImply(newPermission);

      if (!permissionImplicators.isEmpty()) {
         for (Permission implicator : permissionImplicators) {
            LOG.info("The security permission, " + newPermission.toString() + " is implied by the permission, " + implicator.toString() + ".");
         }
      }
      
      LOG.info("Adding security permission, " + newPermission.toString());
      permissions.add(newPermission);

   }

   @Override
   public void addAll(PermissionCollection permissionsToMerge) {
      final Enumeration<Permission> mergingPermissionEnumeration = permissionsToMerge.elements();

      while (mergingPermissionEnumeration.hasMoreElements()) {
         add(mergingPermissionEnumeration.nextElement());
      }
   }

   @Override
   public Enumeration<Permission> elements() {
      return Collections.enumeration(permissions);
   }

   @Override
   public boolean implies(Permission permission) {
      return !getPermissionsThatImply(permission).isEmpty();
   }

   private List<Permission> getPermissionsThatImply(Permission permissionToCheck) {
      final List<Permission> permissionImplicators = new LinkedList<Permission>();

      for (Permission acceptedPermission : permissions) {
         if (acceptedPermission.implies(permissionToCheck)) {
            permissionImplicators.add(acceptedPermission);
         }
      }

      return permissionImplicators;
   }
}
