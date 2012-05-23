package com.rackspace.papi.service.classloader;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoader;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component("classLoaderManager")
public class ApplicationClassLoaderManagerImpl implements ClassLoaderManagerService {

    private final Map<String, EarClassLoaderContext> classLoaderMap;

    public ApplicationClassLoaderManagerImpl() {
        this.classLoaderMap = new HashMap<String, EarClassLoaderContext>();
    }

   @Override
    public synchronized void removeApplication(String contextName) {
        classLoaderMap.remove(contextName);
    }

   @Override
    public synchronized void putApplication(String contextName, EarClassLoaderContext context) {
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
    public boolean hasFilter(String filterName) {
        for (EarClassLoaderContext ctx : classLoaderMap.values()) {
            for (String ctxFilterName : ctx.getEarDescriptor().getRegisteredFilters().keySet()) {
                if (ctxFilterName.equals(filterName)) {
                    return true;
                }
            }
        }
        
        return false;
    }


    @Override
    public synchronized EarClassLoader getApplication(String contextName) {
        final EarClassLoaderContext ctx = classLoaderMap.get(contextName);

        return ctx != null ? ctx.getClassLoader() : null;
    }

    @Override
    public synchronized Collection<EarClassLoaderContext> getLoadedApplications() {
        return Collections.unmodifiableCollection(classLoaderMap.values());
    }
}
