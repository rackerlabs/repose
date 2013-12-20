package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.deploy.ApplicationDeploymentEvent;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventListener;
import com.rackspace.papi.service.event.common.EventService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;

@Component("classLoaderServiceContext")
public class ClassLoaderServiceContext implements ServiceContext<ClassLoaderManagerService> {

    public static final String SERVICE_NAME = "powerapi:/kernel/classloader";
    private final ClassLoaderManagerService classLoaderContext;
    private final ServiceRegistry registry;
    private final EventService eventService;

    @Autowired
    public ClassLoaderServiceContext(
            @Qualifier("classLoaderManager") ClassLoaderManagerService classLoaderContext,
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("eventManager") EventService eventSerivce) {
        this.classLoaderContext = classLoaderContext;
        this.registry = registry;
        this.eventService = eventSerivce;
    }

    private void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public ClassLoaderManagerService getService() {
        return classLoaderContext;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        eventService.listen(
                new EventListener<ApplicationDeploymentEvent, List<EarClassLoaderContext>>() {

                    @Override
                    public void onEvent(Event<ApplicationDeploymentEvent, List<EarClassLoaderContext>> e) {
                        final List<EarClassLoaderContext> contexts = e.payload();
                        final List<String> applications = new ArrayList<String>();

                        for (EarClassLoaderContext ctx : contexts) {
                            classLoaderContext.putApplication(ctx.getEarDescriptor().getApplicationName(), ctx);
                            applications.add(ctx.getEarDescriptor().getApplicationName());
                        }
                        e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, applications);
                    }
                }, ApplicationDeploymentEvent.APPLICATION_LOADED);

        eventService.listen(
                new EventListener<ApplicationDeploymentEvent, String>() {

                    @Override
                    public void onEvent(Event<ApplicationDeploymentEvent, String> e) {
                        final List<String> applications = new ArrayList<String>();
                        classLoaderContext.removeApplication(e.payload());
                        applications.add(e.payload());
                        e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, applications);
                    }
                }, ApplicationDeploymentEvent.APPLICATION_DELETED);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        classLoaderContext.destroy();
    }
}
