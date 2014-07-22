package com.rackspace.papi.service.classloader.ear;

import com.rackspace.papi.service.classloader.archive.ArchiveEntryHelper;

public interface EarArchiveEntryHelper extends ArchiveEntryHelper {

    EarClassLoaderContext getClassLoaderContext();
}
