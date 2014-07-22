package com.rackspace.papi.service.classloader;

import com.rackspace.papi.service.classloader.ear.EarClassLoader;
import com.rackspace.papi.service.classloader.ear.EarClassLoaderContext;

import java.util.Collection;

public interface ClassLoaderManagerService {

    EarClassLoader getApplication(String contextName);
    boolean hasFilter(String contextName);
    Collection<EarClassLoaderContext> getLoadedApplications();
    void removeApplication(String contextName);
    void putApplication(String contextName, EarClassLoaderContext context);
}
