package org.openrepose.commons.utils.classloader;

public interface EarClassLoaderContext {

    EarDescriptor getEarDescriptor();

    ClassLoader getClassLoader();
}
