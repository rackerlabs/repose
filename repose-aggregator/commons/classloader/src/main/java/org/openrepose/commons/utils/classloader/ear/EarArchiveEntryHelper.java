package org.openrepose.commons.utils.classloader.ear;

import org.openrepose.commons.utils.plugin.archive.ArchiveEntryHelper;

public interface EarArchiveEntryHelper extends ArchiveEntryHelper {

    EarClassLoaderContext getClassLoaderContext();
}
