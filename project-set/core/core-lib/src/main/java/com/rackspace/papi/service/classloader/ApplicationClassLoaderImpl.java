package com.rackspace.papi.service.classloader;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoader;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ApplicationClassLoaderImpl implements ApplicationClassLoader, Destroyable {

    private final Map<String, EarClassLoaderContext> classLoaderMap;

    public ApplicationClassLoaderImpl() {
        this.classLoaderMap = new HashMap<String, EarClassLoaderContext>();
    }

    public synchronized void putContext(String contextName, EarClassLoaderContext context) {
        classLoaderMap.put(contextName, context);
    }

    @Override
    public synchronized void destroy() {
        // There is no need to destroy the internal references of the map - the
        // class loaders can not be de-referenced until all active instances of
        // classes they manage are de-referenced. That may take a while so we'll
        // just remove our references to the class loaders.
        classLoaderMap.clear();
    }

    @Override
    public EarClassLoader getApplication(String contextName) {
        final EarClassLoaderContext ctx = classLoaderMap.get(contextName);

        return ctx != null ? ctx.getClassLoader() : null;
    }

    @Override
    public Collection<EarClassLoaderContext> getLoadedApplications() {
        synchronized (classLoaderMap) {
            return Collections.unmodifiableCollection(classLoaderMap.values());
        }
    }
}
