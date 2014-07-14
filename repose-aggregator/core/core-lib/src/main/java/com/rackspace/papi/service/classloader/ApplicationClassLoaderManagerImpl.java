package com.rackspace.papi.service.classloader;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoader;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.service.deploy.ApplicationDeploymentEvent;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventService;
import javax.inject.Inject;
import javax.inject.Named;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Named
public class ApplicationClassLoaderManagerImpl implements ClassLoaderManagerService {

    private final EventService eventService;

    private ConcurrentMap<String, EarClassLoaderContext> classLoaderMap;

    @Inject
    public ApplicationClassLoaderManagerImpl(EventService eventService) {
        this.eventService = eventService;
        this.classLoaderMap = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void afterPropertiesSet() {
        eventService.listen(
                new com.rackspace.papi.service.event.common.EventListener<ApplicationDeploymentEvent, List<EarClassLoaderContext>>() {

                    @Override
                    public void onEvent(Event<ApplicationDeploymentEvent, List<EarClassLoaderContext>> e) {
                        final List<EarClassLoaderContext> contexts = e.payload();
                        final List<String> applications = new ArrayList<String>();

                        for (EarClassLoaderContext ctx : contexts) {
                            putApplication(ctx.getEarDescriptor().getApplicationName(), ctx);
                            applications.add(ctx.getEarDescriptor().getApplicationName());
                        }
                        e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, applications);
                    }
                }, ApplicationDeploymentEvent.APPLICATION_LOADED);

        eventService.listen(
                new com.rackspace.papi.service.event.common.EventListener<ApplicationDeploymentEvent, String>() {

                    @Override
                    public void onEvent(Event<ApplicationDeploymentEvent, String> e) {
                        final List<String> applications = new ArrayList<String>();
                        removeApplication(e.payload());
                        applications.add(e.payload());
                        e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, applications);
                    }
                }, ApplicationDeploymentEvent.APPLICATION_DELETED);
    }

    @PreDestroy
    public void destroy() {
        // There is no need to destroy the internal references of the map - the
        // class loaders can not be de-referenced until all active instances of
        // classes they manage are de-referenced. That may take a while so we'll
        // just remove our references to the class loaders.
        classLoaderMap.clear();
    }

    @Override
    public void removeApplication(String contextName) {
        classLoaderMap.remove(contextName);
    }

    @Override
    public void putApplication(String contextName, EarClassLoaderContext context) {
        classLoaderMap.put(contextName, context);
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
    public EarClassLoader getApplication(String contextName) {
        final EarClassLoaderContext ctx = classLoaderMap.get(contextName);

        return ctx != null ? ctx.getClassLoader() : null;
    }

    @Override
    public Collection<EarClassLoaderContext> getLoadedApplications() {
        //This is legit, as values will return a modifyable thing, we probably don't want them to do this
        return Collections.unmodifiableCollection(classLoaderMap.values());
    }
}
