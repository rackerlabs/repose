package com.rackspace.papi.commons.util.classloader.ear;

import com.rackspace.papi.commons.util.plugin.archive.ArchiveEntryHelper;

public interface EarArchiveEntryHelper extends ArchiveEntryHelper {

    EarClassLoaderContext getClassLoaderContext();
}
