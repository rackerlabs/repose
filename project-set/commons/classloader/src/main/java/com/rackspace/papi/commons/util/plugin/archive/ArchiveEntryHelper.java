package com.rackspace.papi.commons.util.plugin.archive;

import java.util.jar.Manifest;

public interface ArchiveEntryHelper {
    
    EntryAction nextJarEntry(ArchiveEntryDescriptor je);

    void newJarManifest(ArchiveEntryDescriptor name, Manifest manifest);

    void newClass(ArchiveEntryDescriptor name, byte[] classBytes);

    void newResource(ArchiveEntryDescriptor name, byte[] resourceBytes);
}
