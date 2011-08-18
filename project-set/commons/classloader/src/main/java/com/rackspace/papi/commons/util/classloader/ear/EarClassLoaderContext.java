package com.rackspace.papi.commons.util.classloader.ear;

public interface EarClassLoaderContext {

    EarDescriptor getEarDescriptor();

    EarClassLoader getClassLoader();
}
