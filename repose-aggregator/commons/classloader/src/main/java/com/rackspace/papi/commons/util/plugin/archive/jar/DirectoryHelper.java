package com.rackspace.papi.commons.util.plugin.archive.jar;

import java.io.File;

/**
 * This class creates the directory into which Power API artifacts will be deployed (target directory). First the class
 * checks to ensure the root deployment directory exists, is writable, and executable. Next the class creates the
 * deployment directory inside the root directory. Finally, the class sets the permissions on the deployment directory
 * so that only the user can read, write, and execute against the deployment directory (i.e. chmod 700).
 */
public class DirectoryHelper {

   private final File directoryFile;

   public DirectoryHelper(File directoryFile) {
      this.directoryFile = directoryFile;
   }

   public boolean exists() {
      return directoryFile.exists();
   }

   public boolean createTargetDirectory() {
      if (directoryFile.mkdirs()) {
         directoryFile.setReadable(false, false);
         directoryFile.setReadable(true, true);

         directoryFile.setExecutable(false, false);
         directoryFile.setExecutable(true, true);

         return true;
      }

      return false;
   }
}
