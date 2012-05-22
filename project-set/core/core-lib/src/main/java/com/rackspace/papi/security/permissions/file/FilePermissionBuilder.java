package com.rackspace.papi.security.permissions.file;

import com.rackspace.papi.commons.util.StringUtilities;

import java.io.File;
import java.io.FilePermission;
import java.security.Permission;
import java.security.Permissions;

/**
 *
 * @author zinic
 */
public final class FilePermissionBuilder {

   public static final String ANY_FILE = "<<ALL FILES>>";
   public static final String TEMP_DIR_PROPERTY = "java.io.tmpdir";
   
   private static final FilePermissionBuilder INSTANCE = new FilePermissionBuilder();
   
   public static FilePermissionBuilder getInstance() {
      return INSTANCE;
   }

   private FilePermissionBuilder() {
   }

   public Permission buildPermissionForAll(FileSystemAction... allowedActions) {
      return new FilePermission(ANY_FILE, FileSystemAction.actionsToString(allowedActions));
   }

   public Permission buildPermissionFor(String filePath, FileSystemAction... allowedActions) {
      return buildPermissionFor(new File(filePath), allowedActions);
   }

   public Permission buildPermissionFor(File f, FileSystemAction... allowedActions) {
      return new FilePermission(f.getAbsolutePath(), FileSystemAction.actionsToString(allowedActions));
   }

   public Permissions getDefaultFilePermissions() {
      final Permissions defaultFilePermissions = new Permissions();
      final String tempDirString = System.getProperty(TEMP_DIR_PROPERTY);

      if (StringUtilities.isNotBlank(tempDirString)) {
         defaultFilePermissions.add(
                 buildPermissionFor(tempDirString, FileSystemAction.READ, FileSystemAction.WRITE, FileSystemAction.DELETE));
      }

      defaultFilePermissions.add(buildPermissionForAll(FileSystemAction.READ));

      return defaultFilePermissions;
   }
}
