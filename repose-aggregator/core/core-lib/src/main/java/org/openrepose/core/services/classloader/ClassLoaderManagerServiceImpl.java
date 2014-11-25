package org.openrepose.core.services.classloader;

import org.openrepose.commons.utils.classloader.ear.EarClassLoader;
import org.openrepose.commons.utils.classloader.ear.EarClassLoaderContext;
import org.openrepose.core.services.deploy.ApplicationDeploymentEvent;
import org.openrepose.core.services.event.common.Event;
import org.openrepose.core.services.event.common.EventListener;
import org.openrepose.core.services.event.common.EventService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

@Named
public class ClassLoaderManagerServiceImpl implements ClassLoaderManagerService {

    private final EventService eventService;
    private final Map<String, EarClassLoaderContext> classLoaderMap;

    @Inject
    public ClassLoaderManagerServiceImpl(EventService eventService) {
        this.eventService = eventService;
        this.classLoaderMap = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        eventService.listen(
                new EventListener<ApplicationDeploymentEvent, List<EarClassLoaderContext>>() {

                    @Override
                    public void onEvent(Event<ApplicationDeploymentEvent, List<EarClassLoaderContext>> e) {
                        final List<EarClassLoaderContext> contexts = e.payload();
                        final List<String> applications = new ArrayList<>();

                        for (EarClassLoaderContext ctx : contexts) {
                            putApplication(ctx.getEarDescriptor().getApplicationName(), ctx);
                            applications.add(ctx.getEarDescriptor().getApplicationName());
                        }
                        e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, applications);
                    }
                }, ApplicationDeploymentEvent.APPLICATION_LOADED);

        eventService.listen(
                new EventListener<ApplicationDeploymentEvent, String>() {

                    @Override
                    public void onEvent(Event<ApplicationDeploymentEvent, String> e) {
                        final List<String> applications = new ArrayList<>();
                        removeApplication(e.payload());
                        applications.add(e.payload());
                        e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, applications);
                    }
                }, ApplicationDeploymentEvent.APPLICATION_DELETED);
    }

    @PreDestroy
    public synchronized void destroy() {
        // There is no need to destroy the internal references of the map - the
        // class loaders can not be de-referenced until all active instances of
        // classes they manage are de-referenced. That may take a while so we'll
        // just remove our references to the class loaders.
        classLoaderMap.clear();
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
