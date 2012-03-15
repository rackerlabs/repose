package com.rackspace.papi.commons.util.plugin.archive;

public class ArchiveEntryDescriptor {

   public static final String ROOT_ARCHIVE = "ROOT";
   private final String entryName, entryPrefix, simpleName, extension, archiveName;

   public ArchiveEntryDescriptor(String archiveName, String entryName, String entryPrefix, String simpleName, String extension) {
      this.archiveName = archiveName;
      this.entryPrefix = entryPrefix;
      this.simpleName = simpleName;
      this.extension = extension;
      this.entryName = entryName;
   }

   public boolean isRootArchiveEntry() {
      return archiveName.equals(ROOT_ARCHIVE);
   }

   public String getArchiveName() {
      return archiveName;
   }

   public String fullName() {
      return entryName;
   }

   public String getPrefix() {
      return entryPrefix;
   }

   public String getSimpleName() {
      return simpleName;
   }

   public String getExtension() {
      return extension;
   }
}
