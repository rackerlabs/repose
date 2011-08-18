package com.rackspace.papi.service.classloader;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoader;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import java.util.Collection;

public interface ApplicationClassLoader {

    EarClassLoader getApplication(String contextName);

    Collection<EarClassLoaderContext> getLoadedApplications();
}
