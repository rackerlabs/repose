package com.rackspace.papi.commons.util.classloader.ear;

import com.rackspace.papi.commons.util.plugin.archive.ArchiveEntryListener;

public interface EarArchiveEntryListener extends ArchiveEntryListener {

    EarClassLoaderContext getClassLoaderContext();
}
