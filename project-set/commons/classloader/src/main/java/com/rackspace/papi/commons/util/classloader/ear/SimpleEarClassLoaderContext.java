package com.rackspace.papi.commons.util.classloader.ear;

import com.rackspace.papi.commons.util.plugin.archive.ArchiveEntryDescriptor;
import java.io.File;

public class SimpleEarClassLoaderContext implements EarClassLoaderContext {

    private final EarClassLoader childContext, parentContext;
    private final EarDescriptor earDescriptor;

    public SimpleEarClassLoaderContext(File deploymentRoot) {
        earDescriptor = new EarDescriptor();

        parentContext = new EarClassLoader(deploymentRoot);
        childContext = new EarClassLoader(parentContext, deploymentRoot);
    }

    public SimpleEarClassLoaderContext(ClassLoader absoluteParent, File deploymentRoot) {
        earDescriptor = new EarDescriptor();

        parentContext = new EarClassLoader(absoluteParent, deploymentRoot);
        childContext = new EarClassLoader(parentContext, deploymentRoot);
    }

    public EarClassLoader getClassLoaderForEntry(ArchiveEntryDescriptor entryDescriptor) {
        final String name = entryDescriptor.fullName();

        if (entryDescriptor.isRootArchiveEntry() && (name.startsWith("lib/") || name.startsWith("APP-INF/lib"))) {
            return parentContext;
        }

        return childContext;
    }

    @Override
    public EarClassLoader getClassLoader() {
        return childContext;
    }

    @Override
    public EarDescriptor getEarDescriptor() {
        return earDescriptor;
    }
}
