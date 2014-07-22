package com.rackspace.papi.service.classloader;

import com.rackspace.papi.service.classloader.archive.ArchiveEntryDescriptor;

import java.util.Arrays;

public class ResourceDescriptor {

   private final ArchiveEntryDescriptor descriptor;
   private final byte[] digestBytes;

   public ResourceDescriptor(ArchiveEntryDescriptor descriptor, byte[] digestBytes) {
      //TODO: add guard to ensure digestBytes data is not empty?
      this.descriptor = descriptor;
      this.digestBytes = Arrays.copyOf(digestBytes, digestBytes.length);
   }

   public byte[] digestBytes() {
      return (byte[])digestBytes.clone();
   }

   public ArchiveEntryDescriptor archiveEntry() {
      return descriptor;
   }
}
