package com.rackspace.papi.commons.util.plugin.archive;

import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public final class ManifestProcessor {

   public static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

   private ManifestProcessor() {
   }

   public static void processManifest(ArchiveEntryDescriptor archiveEntryDescriptor, JarInputStream inputStream, ArchiveEntryHelper helper) {
      final Manifest manifest = inputStream.getManifest();

      if (manifest != null) {
         helper.newJarManifest(archiveEntryDescriptor, manifest);
      }
   }
}
