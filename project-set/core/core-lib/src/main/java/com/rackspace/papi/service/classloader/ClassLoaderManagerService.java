package com.rackspace.papi.service.classloader;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoader;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;

import java.util.Collection;

public interface ClassLoaderManagerService extends Destroyable {

    EarClassLoader getApplication(String contextName);
    boolean hasFilter(String contextName);
    Collection<EarClassLoaderContext> getLoadedApplications();
    void removeApplication(String contextName);
    void putApplication(String contextName, EarClassLoaderContext context);
}
