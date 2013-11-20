package com.rackspace.papi.security;

import com.rackspace.papi.security.permissions.file.FilePermissionBuilder;
import com.rackspace.papi.security.permissions.file.FileSystemAction;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.Permission;

import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class DelegatingSecurityManagerTest {

   public static class WhenPerformingPermissionChecks {

      @Test
      public void shouldInterceptPermissionCheckMethods() {
         final SecurityPermissions permissions = mock(SecurityPermissions.class);
         when(permissions.implies(any(Permission.class))).thenReturn(true);

         final DelegatingSecurityManager realManager = new DelegatingSecurityManager(permissions);
         realManager.checkRead("/some/File");
         verify(permissions, atLeastOnce()).implies(any(FilePermission.class));
         
         realManager.checkConnect("localhost", 80);
         verify(permissions, atLeastOnce()).implies(any(SocketPermission.class));
      }
   }

   public static class WhenDelegatingPermissionChecks {

      @Test(expected = SecurityException.class)
      public void shouldNotPassPermissionChecksToNullDelegatedSecurityManagers() {
         final SecurityPermissions permissions = mock(SecurityPermissions.class);
         when(permissions.implies(any(Permission.class))).thenReturn(false);

         final DelegatingSecurityManager realManager = new DelegatingSecurityManager(permissions);
         final Permission permission = FilePermissionBuilder.getInstance().buildPermissionFor("/some/file", FileSystemAction.READ);

         realManager.checkPermission(permission);
      }

      @Test
      public void shouldPassPermissionChecksToDelegatedSecurityManager() {
         final SecurityManager mockedManager = mock(SecurityManager.class);
         final DelegatingSecurityManager realManager = new DelegatingSecurityManager(new SecurityPermissions(), mockedManager);

         final Permission permission = FilePermissionBuilder.getInstance().buildPermissionFor("/some/file", FileSystemAction.READ);

         realManager.checkPermission(permission);

         verify(mockedManager, times(1)).checkPermission(permission);
      }
   }
}
