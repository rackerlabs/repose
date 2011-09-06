package com.rackspace.papi.service.classloader;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoader;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import java.util.Collection;

public interface ApplicationClassLoaderManager {

    EarClassLoader getApplication(String contextName);
    
    boolean hasFilter(String contextName);

    Collection<EarClassLoaderContext> getLoadedApplications();
}
