package com.rackspace.papi.service.classloader.ear;

public interface EarClassLoaderContext {

    EarDescriptor getEarDescriptor();

    EarClassLoader getClassLoader();
}
