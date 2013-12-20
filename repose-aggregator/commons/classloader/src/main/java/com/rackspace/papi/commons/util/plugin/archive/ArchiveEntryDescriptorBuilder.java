package com.rackspace.papi.commons.util.plugin.archive;

import com.rackspace.papi.commons.util.StringUtilities;

public final class ArchiveEntryDescriptorBuilder {

   private ArchiveEntryDescriptorBuilder() {
   }

   public static ArchiveEntryDescriptor build(String archiveName, String entryName) {
      final String sanitizedEntryName = entryName; 
      final int lastSeperatorIndex = sanitizedEntryName.lastIndexOf('/');

      final String resourceName = lastSeperatorIndex > 0 ? sanitizedEntryName.substring(lastSeperatorIndex + 1, sanitizedEntryName.length()) : sanitizedEntryName;
      final String resourcePath = lastSeperatorIndex > 0 ? sanitizedEntryName.substring(0, lastSeperatorIndex + 1) : "";

      final int extensionIndex = resourceName.lastIndexOf('.');

      ArchiveEntryDescriptor archiveEntryDescriptor;

      if (extensionIndex > 0) {
         final String simpleName = resourceName.substring(0, extensionIndex);
         final String extension = resourceName.substring(extensionIndex + 1, resourceName.length());

         archiveEntryDescriptor = new ArchiveEntryDescriptor(archiveName, StringUtilities.trim(sanitizedEntryName, "/"), resourcePath, simpleName, extension);
      } else {
         archiveEntryDescriptor = new ArchiveEntryDescriptor(archiveName, StringUtilities.trim(sanitizedEntryName, "/"), resourcePath, resourceName, "");
      }

      return archiveEntryDescriptor;
   }
}
