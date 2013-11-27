package com.rackspace.papi.commons.util.classloader.ear;

import com.rackspace.papi.commons.util.plugin.archive.ArchiveEntryDescriptor;

import java.io.File;
import java.security.PrivilegedAction;

import static java.security.AccessController.doPrivileged;

public class SimpleEarClassLoaderContext implements EarClassLoaderContext {

    private EarClassLoader childContext;
    private EarClassLoader parentContext;
    private final EarDescriptor earDescriptor;

    public SimpleEarClassLoaderContext(final File deploymentRoot) {
        earDescriptor = new EarDescriptor();

        doPrivileged(new PrivilegedAction() {
            public Object run() {
                parentContext = new EarClassLoader(deploymentRoot);
                childContext = new EarClassLoader(parentContext, deploymentRoot);
                return null;
            }
        });
    }

    public SimpleEarClassLoaderContext(final ClassLoader absoluteParent, final File deploymentRoot) {
        earDescriptor = new EarDescriptor();

        doPrivileged(new PrivilegedAction() {
            public Object run() {
                parentContext = new EarClassLoader(absoluteParent, deploymentRoot);
                childContext = new EarClassLoader(parentContext, deploymentRoot);
                return null;
            }
        });
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
