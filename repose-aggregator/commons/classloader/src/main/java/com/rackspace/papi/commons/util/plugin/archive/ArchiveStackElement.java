package com.rackspace.papi.commons.util.plugin.archive;

import java.util.jar.JarInputStream;

public class ArchiveStackElement {

   private final JarInputStream inputStreamReference;
   private final String archiveName;

   public ArchiveStackElement(JarInputStream inputStreamReference, String archiveName) {
      this.inputStreamReference = inputStreamReference;
      this.archiveName = archiveName;
   }

   public String getArchiveName() {
      return archiveName;
   }

   public JarInputStream getInputStream() {
      return inputStreamReference;
   }
}
